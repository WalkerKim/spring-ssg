# Spring SSG - Spring MVC Static Site Generator

## What is Spring SSG
Spring SSG는 Spring Framework를 사용하면서 정적 사이트를 생성하기 위한 프로젝트.

JSP, Thymeleaf등의 템플릿 엔진에 익숙한 Spring 개발자들에게 정적 페이지를 도출하는 옵션을 제공한다.
## Why Spring SSG
Spring을 주력으로 사용하는 개발자들이 개인 소개 페이지, 회사 소개 페이지, 간단한 랜딩 페이지를 작성해 배포한다고 할 때 템플릿 엔진을 탑제한 Spring Boot 앱을 구동해서 배포하게 되고 매우 불필요한 컴퓨팅 파워와 비용 낭비를 하게 된다. AWS S3 요금과 EC2의 가격차이를 생각하다 보면 새로운 아이디어를 현실로 옮기는데 장애물이 되기도 한다.

Spring SSG는 Java Spring 개발자도 새로운 기술 학습이나 순수 Html작성 과 같은 노가다성 작업 없이 익숙한 템플릿 개발 환경이나 기존 프로젝트를 사용해서 정적 페이지를 작성하고 배포할 수 있도록 도와준다.

## How Spring SSG works
기본 원리는 RestTemplate을 통해서 작동중인 Spring Application에 질의를 하고 그 결과물을 html파일로 추출하고 static 경로에 있는 정적 파일들과 함께 특정 디렉토리에 복사하는 것이다.  

질의를 위해 Spring SSG에 있는 HtmlGenerator객체를 생성하면서 requestMapping 정보인 HandlerMapping Bean 리스트와 static 리소스 복사를 위해 필요한 Bean 인 ResourceUrlProvider를 생성자로 전달한다.
전달받은 내용을 통해 질의를 하고 결과물을 추출한다.

## Requirements
Spring Web MVC

## Using Spring SSG

### Add Spring SSG dependency
```xml
        <dependency>
            <groupId>kim.figure</groupId>
            <artifactId>spring-ssg</artifactId>
            <version>3.0.0-SNAPSHOT</version>
        </dependency>
```

### HtmlGenerator Bean 등록
Static site를 생성하기 위해 HtmlGenerator Bean을 등록한다.
(Spring SSG Spring Boot Starter)[https://github.com/WalkerKim/spring-ssg-spring-boot-starter]를 사용할 경우 자동으로 해당 빈을 등록한다.

생성자의 인자로는 ResourceUrlProvider, static 파일 및 질의 결과물이 복사될 디렉토리, HandlerMapping Bean 리스트, 포트번호, ApplicationContext, view에 대한 결과물로 .html 확장자를 포함할지 여부를 전달한다.

일반적인 방법으로 @Controller에서 @RequestMapping 또는 @GetMapping을 통해서만 HandlerMapping을 한 경우라면 RequestMappingHandlerMapping Bean만 전달하면 된다. 추가로 특정 설정등을 통해 HandlerMapping이 추가되었다면 해당 Bean을 의존주입 받아 List에 추가해서 넘겨야 한다.

```java
import kim.figure.springssg.HtmlGenerator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.context.ApplicationContext;

@Configuration
public class SpringSsgConfiguration {
    @Value("${server.port:8080}")
    String port;

    @Value("${dist.path:dist}")
    String distPath;

    @Autowired
    RequestMappingHandlerMapping requestMappingHandlerMapping;
    
    @Autowired
    HandlerMapping viewControllerHandlerMapping;

    @Autowired
    ResourceUrlProvider resourceUrlProvider;

    @Autowired
    ApplicationContext applicationContext;


    @Bean
    public HtmlGenerator htmlGenerator(){
        HtmlGenerator htmlGenerator = new HtmlGenerator(resourceUrlProvider, distPath, List.of(requestMappingHandlerMapping, viewControllerHandlerMapping), "8080", applicationContext, true);
        htmlGenerator.generateStaticSite();
    }
}
```

### Controller에 적용
위의 서비스를 등록 한 후 정적 추출의 대상이 되는 메소드에 @EnableSsg를 추가한다. (class 전체 GET Mapping된 메소드에 적용하려면(param, pathVariable없는 메소드) Class레벨에 @EnableSsgAllGetMethodWithoutParamAndPathVariable을 추가한다.)

PathVariable이 없는 경우에는 @EnableSsg만 추가하면 된다.
```java
import org.springframework.stereotype.Controller;

@Controller
public class PageController {
    @GetMapping("/")
    @EnableSsg
    public String index(Model model){
        return"index";
    }
    
}

```
PathVariable이 있는 경우에는 Path name과 그에 대한 value를 List<Map<String,String>>형식으로 반환하는 메소드의 이름과 그 메소드를 포함한 Bean Class정보를 @EnableSsg의 인자로 전달한다.

```java
import kim.figure.site.main.category.CategoryService;
import kim.figure.springssg.EnableSsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PostController {
    @Autowired
    PostService postService;
    @Autowired
    CategoryService categoryService;

    @GetMapping("/post/{id}")
    @EnableSsg(pathVariableBeanRepositoryClass = PostService.class, getPathVariableListMethodName = "getAllIdList")
    public String post(Model model, @PathVariable("id") Long id){
        PostDto.Get post = postService.getPost(id);
        LinkedMultiValueMap<String, String> test = new LinkedMultiValueMap<>();
        model.addAttribute("post", post);
        model.addAttribute("recommendPostList", postService.getRecommendPostList(id));
        return "post/post";
    }
}
```
### PathVariable Bean Repository Class
위에서 전달한 getAllIdList 메소드를 포함하는 PostService는 Bean으로 등록되어 있어야 한다.
```java
@Service
public class PostService {
    
    @Autowired
    PostRepository postRepository;
    
    public List<Map<String,String>> getAllIdList(){

        return postRepository.findByIsPublished(true).stream().map(i -> {
            Map<String, String> map = new HashMap();
            map.put("id", i.getId().toString());
            return map;
        }).collect(Collectors.toList());

    }
}    
```
### Run Spring SSG
앞서 등록한 HtmlGenerator generateStaticSite()를 실행하면 정적 파일 생성 작업이 진행된다.

