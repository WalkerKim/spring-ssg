
# Spring SSG - Spring MVC Static Site Generator

## What is Spring SSG
Spring SSG is a project that generates static sites using Spring Framework.
For Developer who is familiar with JSP, Thymeleaf and other template engines, Spring SSG provides an option to generate static pages.

## Why Spring SSG
If a Spring developer wants to build and deploy an About Me page, About Us page, or a simple landing page, they'll be running a Spring Boot app topped with a template engine, wasting a lot of unnecessary computing power and money. The price difference between AWS S3 and EC2 can also be a barrier to turning new ideas into reality.

Spring SSG helps Java Spring developers create and deploy static pages using familiar template development environments or existing projects, without having to learn new technologies or engage in tedious manual tasks such as writing pure HTML.


## How Spring SSG works
The basic principle is to query a working Spring Application through a RestTemplate and extract the result as an html file and copy it to a specific directory along with the static files in the static path.

To make a query, we create an HtmlGenerator object in the Spring SSG and pass a list of HandlerMapping beans, which is the requestMapping information, and a ResourceUrlProvider, which is the bean we need to copy the static resource, to the generator.
We make queries and extract the results through the passed in contents.

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

### Spring SSG service
Register the HtmlGenerator Bean to generate the static site.
If you use the (Spring SSG Spring Boot Starter)[https://github.com/WalkerKim/spring-ssg-spring-boot-starter], the bean is automatically registered.

The arguments to the constructor are a ResourceUrlProvider, a directory to copy static files and query results to, a list of HandlerMapping Beans, a port number, an ApplicationContext, and whether to include the .html extension as the result for the view.

In general, if you are only HandlerMapping via @RequestMapping or @GetMapping from @Controller, you only need to pass the RequestMappingHandlerMapping Bean. If additional HandlerMappings are added through specific configurations, you will need to depend on those beans, add them to the list, and pass them in.



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

### Apply to Controller
After registering the above service, add @EnableSsg to the methods that are subject to static extraction. (If you want to apply it to class-wide GET Mapped methods (methods without param and pathVariable), add @EnableSsgAllGetMethodWithoutParamAndPathVariable at the Class level).

If you don't have a PathVariable, you only need to add @EnableSsg.
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
If there is a PathVariable, pass the name of the method that returns the path name and its value as a List<Map<String,String>> and the bean class information including the method as arguments to @EnableSsg.

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
The PostService containing the getAllIdList method passed above must be registered as a bean.
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
Run init() of the StaticGenService bean to create the static file.