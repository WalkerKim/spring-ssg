package kim.figure.springssg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.PathVariableMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


/**
 * The type Html generator.
 *
 * @author : "DoHyeong Walker Kim"
 */
public class HtmlGenerator extends PathVariableMethodArgumentResolver {
    private static final Logger log = LoggerFactory.getLogger(HtmlGenerator.class);
    private final String distributionPath;
    private final ResourceUrlProvider resourceUrlProvider;
    private final List<HandlerMapping> handlerMappingList;
    private final ApplicationContext applicationContext;
    private final Boolean htmlExtensionBoolean;
    private final String port;

    /**
     * Instantiates a new Html generator.
     *
     * @param resourceUrlProvider  the resource url provider
     * @param distributionPath     the distribution path
     * @param handlerMappingList   the handler mapping list
     * @param port                 the port
     * @param applicationContext   the application context
     * @param htmlExtensionBoolian the html extension boolian
     */
    public HtmlGenerator(ResourceUrlProvider resourceUrlProvider, String distributionPath, List<HandlerMapping> handlerMappingList, String port, ApplicationContext applicationContext, Boolean htmlExtensionBoolian) {
        super();
        this.resourceUrlProvider = resourceUrlProvider;
        this.distributionPath = distributionPath;
        this.handlerMappingList = handlerMappingList;
        this.port = port;
        this.applicationContext = applicationContext;
        this.htmlExtensionBoolean = htmlExtensionBoolian;
    }


    /**
     * Generate static site.
     *
     * @throws IOException the io exception
     */
    public void generateStaticSite() throws IOException {
        //Clear exists files in distribution path
        clearDistPath(distributionPath);
        //Copy static resources
        copyStaticResources(distributionPath);
        //Make html by list of request path
        List<String> requestUriList = makeRequestUriByRequestHandlerMapping(handlerMappingList);
        //Make Static Html files by Uri
        makeStaticHtmlByUriRequest(requestUriList);
    }

    private void makeStaticHtmlByUriRequest(List<String> requestUriList) throws UnknownHostException {

        RestTemplate restTemplate = new RestTemplate();
        String host = "http://" + InetAddress.getLocalHost().getHostAddress();

        requestUriList.parallelStream().forEach(uri -> {
            ResponseEntity<String> response = restTemplate.exchange(host + ":" + port + uri, HttpMethod.GET, new HttpEntity<>(null, new HttpHeaders()), String.class);
            if (response.getStatusCode() == HttpStatus.OK) {

            } else {
                log.warn("Request of [" + uri + "] " + "got " + response.getStatusCode() + " so error page will be made");
            }
            Path filePath;
            if (uri.equals("/")) {
                if(htmlExtensionBoolean){
                    filePath = Paths.get(distributionPath + File.separator + "index.html");
                }else{
                    filePath = Paths.get(distributionPath + File.separator + "index");
                }
            } else {
                if(htmlExtensionBoolean){
                    filePath = Paths.get(distributionPath + File.separator + uri + ".html");
                }else{
                    filePath = Paths.get(distributionPath + File.separator + uri + "");
                }
            }
            try {
                if(Files.exists(filePath.getParent())){

                }else{
                    Files.createDirectory(filePath.getParent());
                }
                Files.write(filePath, response.getBody().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    private List<String> makeRequestUriByRequestHandlerMapping(List<HandlerMapping> handlerMappingList) {
        List<String> requestUriList = new ArrayList<>();
        for (HandlerMapping handlerMapping : handlerMappingList) {
            if(handlerMapping instanceof RequestMappingHandlerMapping){
                requestUriList.addAll(requestHandlerMappingUriExtractor((RequestMappingHandlerMapping)handlerMapping));
            }else if(handlerMapping instanceof AbstractUrlHandlerMapping){
                AbstractUrlHandlerMapping mapping = (AbstractUrlHandlerMapping)handlerMapping;
                requestUriList.addAll(mapping.getHandlerMap().entrySet().stream().map(handlerMap -> handlerMap.getKey()).filter(string -> !string.endsWith("/")).collect(Collectors.toList()));
            }
        }
        return requestUriList;
    }

    /**
     * Request handler mapping uri extractor list.
     *
     * @param requestMappingHandlerMapping the request mapping handler mapping
     * @return the list
     */
    List<String> requestHandlerMappingUriExtractor(RequestMappingHandlerMapping requestMappingHandlerMapping){
        return requestMappingHandlerMapping
                .getHandlerMethods()
                .entrySet()
                .stream()
                .filter(filterByAnnotations())
                .flatMap(mappingInfoHandlerMethodEntry -> Arrays.stream(mappingInfoHandlerMethodEntry.getValue().getMethod().getDeclaredAnnotations())
                        // filtering with @EnableSsg
                        .filter(annotation -> annotation instanceof EnableSsg)
                        .flatMap(methodWithEnableSsgAnnotation -> {
                            //test with has path pattern
                            if (!mappingInfoHandlerMethodEntry.getKey().getPathPatternsCondition().getPatterns().stream().map(i -> i.getPatternString()).collect(Collectors.joining("")).contains("{")) {
                                return Arrays.asList(mappingInfoHandlerMethodEntry.getKey().getPathPatternsCondition().getFirstPattern().getPatternString()).stream();
                            } else {
                                // Extract Service bean
                                Object pathVariableRepositoryService = applicationContext.getBean(((EnableSsg) methodWithEnableSsgAnnotation).pathVariableBeanRepositoryClass());
                                // Extract method name from annotation information
                                String methodName = ((EnableSsg) methodWithEnableSsgAnnotation).getPathVariableListMethodName();
                                if (methodName.isEmpty()) {
                                    methodName = mappingInfoHandlerMethodEntry.getValue().getMethod().getName();
                                }
                                List<Map<String, String>> pathMapList = null;
                                Method method = null;
                                try {
                                    // get method from service bean
                                    method = ((EnableSsg) methodWithEnableSsgAnnotation)
                                            .pathVariableBeanRepositoryClass()
                                            .getMethod(methodName);
                                    // get pathVariable combination list by executing method
                                    pathMapList = (List<Map<String, String>>) method.invoke(pathVariableRepositoryService, null);
                                } catch (NoSuchMethodException e) {
                                    e.printStackTrace();
                                } catch (InvocationTargetException e) {
                                    e.printStackTrace();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }

                                return pathMapList.stream().map(pathMap -> {
                                    //mapping pattern
                                    String pattern = mappingInfoHandlerMethodEntry.getKey().getPathPatternsCondition().getFirstPattern().getPatternString();
                                    //replace path pattern to value
                                    for (Map.Entry<String, String> entry : pathMap.entrySet()) {
                                        pattern = pattern.replace("{" + entry.getKey() + "}", entry.getValue());
                                    }
                                    return pattern;
                                });
                            }
                        }))
                .collect(Collectors.toList());

    }

    /**
     * Clear dist path.
     *
     * @param distributionPath the distribution path
     * @throws IOException the io exception
     */
    public void clearDistPath(String distributionPath) throws IOException {
        try {
            if (!Files.exists(Paths.get(distributionPath)))
                Files.createDirectory(Paths.get(distributionPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Files.walk(Paths.get(distributionPath))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    /**
     * Copy static resources.
     *
     * @param distributionPath the distribution path
     */
    public void copyStaticResources(String distributionPath) {
        resourceUrlProvider.getHandlerMap().entrySet().stream().forEach(resourceHandlerEntry -> {
            String uriPattern = resourceHandlerEntry.getKey();
            String distributionPathWithChildren = distributionPath + uriPattern.replace("**", "");
            if (uriPattern.contains("/**")) {
                resourceHandlerEntry.getValue().getLocations().forEach(origin -> {
                    if (origin instanceof ClassPathResource) {
                        ClassPathResource classPathResource = (ClassPathResource) origin;
                        if (classPathResource.exists()) {
                            try {
                                Files.walk(classPathResource.getFile().toPath()).forEach(resource -> {
                                    try {
                                        Path destination = Paths.get(distributionPathWithChildren, resource.toString()
                                                .substring(classPathResource.getFile().toString().length()));
                                        Files.copy(resource, destination, REPLACE_EXISTING);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        throw new RuntimeException(e.getMessage());
                                    }
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                });

            } else {
                log.warn("field to copy of \"" + uriPattern + "\". static resources." + " (support only simply end with /** pattern)");
            }
        });
    }

    private static Predicate<Map.Entry<RequestMappingInfo, HandlerMethod>> filterByAnnotations() {
        return infoHandlerMethodEntry -> {
            // filtering with @EnableSsg annotation
            if (infoHandlerMethodEntry.getValue().hasMethodAnnotation(EnableSsg.class)) {

            } else {

                if (infoHandlerMethodEntry.getValue().getClass().isAnnotationPresent(EnableSsgAllGetMethodWithoutPathVariable.class)) {
                    //no @EnableSsg but @EnableSsgAllGetMethodWithoutParamAndPathVariable annotation on class
                } else {
                    return false;
                }
            }

            if (infoHandlerMethodEntry.getKey().getMethodsCondition().getMethods().isEmpty()) {
                return true;
            } else if (infoHandlerMethodEntry.getKey().getMethodsCondition().getMethods().contains(RequestMethod.GET)) {
                return true;
            } else {
                return false;
            }
        };
    }
    private static class PathVariableNamedValueInfo extends NamedValueInfo {

        /**
         * Instantiates a new Path variable named value info.
         *
         * @param annotation the annotation
         */
        public PathVariableNamedValueInfo(PathVariable annotation) {
            super(annotation.name(), annotation.required(), ValueConstants.DEFAULT_NONE);
        }
    }
}
