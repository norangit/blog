# @ControllerAdvice Spring 全局异常处理，动态返回 JSON错误或者跳转到错误页面



## @ControllerAdvice 简介

Spring 从 3.2 开始提供了一个注解 **@ControllerAdvice** 来处理全局的异常，这个注解有 5 个参数，实际上是 4 个有用的，一个是别名引用。

这 4 个有用的分别是：

1、**basePackages：**String 数组类型，基本路径用于捕获异常。可以指定多个类路径进行限定，该类路径下的所有异常都将被处理。比如：**@ControllerAdvice(basePackages = {"org.my.pkg", "org.my.other.pkg"})**，那么 org.my.pkg、org.my.other.pkg 路径下的所有类抛出的异常都将被捕获处理。

2、**basePackageClasses：**Class<?> 数组类型。和 **basePackages** 类似，不同之处在于，假如包路径为 a.b.c.d，只想处理，a.b，和 c.d 这 2 个包的（不包含子目录）所有异常的时候，**basePackages** 就办不到了，这个时候就可以定义一个标志类或者接口，类似于 **java.io.Serializable** ,它只是用来标志实现了该接口的所有类都可以被序列化。同理，实现上述 2 个路径下的异常处理，可以定义一个标志类，比如就叫 a.b.AExceptionHandler, c.d.BExceptionHandler，即：

**@ControllerAdvice(basePackageClasses = {"a.b.AExceptionHandler", "c.d.BExceptionHandler"})**

3、**assignableTypes：**Class<?> 数组类型。实现或集成了该指定 Class 的类都将被处理，类似于 **java.io.Serializable** ，实现了该接口的所有类都被认为是可以被序列化的。如：

**@ControllerAdvice(assignableTypes = {"java.io.Serializable", "c.d.BExceptionHandler"})**

4、**annotations：** Class<? extends Annotation> 数组类型。这个就更容易理解了，被加注了该指定注解的所有类的异常都将被处理。如：**@ControllerAdvice(annotations = {RestController.class, Controller.class})**，只监听 Controller 层的所有异常。



## Web 应用使用场景

在 Web 应用中，只要对于异常的处理要求不很严格的话，直接限定在 **Controller** 层就可以了，因为所有请求都会经过 **Controller**，如果没有请求进来，基本上可以理解为应用并没有在执行任何业务逻辑（别钻牛角尖）。所以，也没必要监听别的业务层的异常，当然了，具体场景自己选择就是了。

对于一个传统的 Web 应用，前后端代码是整合在一起的，在 **Controller** 里的一个 Mapping 方法的返回结果，可能是跳转到对应页面，也可能是一个 rest 请求，只返回一个 json 字符串。对于单一类型的异常捕获当然很简单了，不用分情况，直接看代码，（MyResponse 后面再看）：

全部返回 **JSON：**

```java
public class GlobalErrorHandler {
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(GlobalErrorHandler.class);
		
  	//所有异常处理结果都返回 json
    @ExceptionHandler(RuntimeException.class)
  	@ResponseBody
    public Object runtimeException(RuntimeException e, HandlerMethod handlerMethod, HttpServletRequest request, HttpServletResponse response) {
        logger.error("运行时异常：", e);
        return MyResponse.error(-100, "运行时异常：" + e.getMessage());
    }
}
```



或者全部返回 **ModelAndView：**

```java
public class GlobalErrorHandler {
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(GlobalErrorHandler.class);
		
  	//所有异常处理结果都返回 json
    @ExceptionHandler(RuntimeException.class)
  	public ModelAndView runtimeException(RuntimeException e, HandlerMethod handlerMethod, HttpServletRequest request, HttpServletResponse response) {
        logger.error("运行时异常：", e);
        ModelAndView modelAndView = new ModelAndView("error");
        response.setStatus(status.value());

        modelAndView.addObject("error", msg);
        modelAndView.addObject("status", status.value());
        modelAndView.addObject("code", code);
        return modelAndView;
    }
}
```



但是实际场景并不是这样的，需要考虑 2 者都存在的情况。所以就需要根据实际的 **Mappring** 方法的返回类型来做出对应的响应。可以约定所有 **Controller** 只有 2 中返回值类型，一种是自定义的 json 返回类型，一种是 **ModelAndView**。

自定义的 json 返回类型，就是上面提到的 **MyResponse**。为了格式统一，方便前后端开发，可以约定自定义一个返回对象，这个对象只有 3 个属性：

**code：** 状态码，成功用 0 表示，因为成功只有一种，失败有很多种，所以用 0 表示成功。

**msg：**返回消息，简单的比如："请求成功"、"请求错误"。

**result：**用于存放数据，成功时的实际数据。至于业务逻辑失败，看具体需求给结果即可。

如下：

```java
public class MyResponse {
    private String msg;
    private int code;
    private Object result;

    private MyResponse(){

    }

    public static MyResponse error(int code, String message) {
        MyResponse myResponse = new MyResponse();
        myResponse.setCode(code);
        myResponse.setMsg(message);
        return myResponse;
    }

    public static MyResponse success() {
        MyResponse myResponse = new MyResponse();
        myResponse.setCode(0);
        myResponse.setMsg("Success");
        myResponse.setResult(new Empty());

        return myResponse;
    }

    public static MyResponse success(Object data) {
        MyResponse myResponse = new MyResponse();
        myResponse.setCode(0);
        myResponse.setMsg("Success");
        myResponse.setResult(data);

        return myResponse;
    }
		
		//定义个空对象，避免前端无畏的 undefined 错误
    private static class Empty {
    }
}
```



来看如何动态根据实际的 **Mappring** 方法的返回类型来做出对应的响应：

```java
@ControllerAdvice(annotations = {RestController.class, Controller.class})
public class GlobalErrorHandler {
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(GlobalErrorHandler.class);
		
  	//根据实际需要，可以添加多个特定的异常类型
  	//...
  
    @ExceptionHandler(JsonProcessingException.class)
    public Object jsonProcessingException(JsonProcessingException e, HandlerMethod handlerMethod, HttpServletRequest request, HttpServletResponse response) {
        logger.error("JSON 解析失败：", e);
        return handle(-2, "JSON 解析失败：", handlerMethod, request, response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Object jsonProcessingException(ConstraintViolationException e, HandlerMethod handlerMethod, HttpServletRequest request, HttpServletResponse response) {
        logger.error("表单校验失败：", e.getLocalizedMessage());

        return handle(-2, "表单校验失败", handlerMethod, request, response);
    }

    @ExceptionHandler(RuntimeException.class)
    public Object runtimeException(RuntimeException e, HandlerMethod handlerMethod, HttpServletRequest request, HttpServletResponse response) {
        logger.error("运行时异常：", e);
        return handle(-11, "运行时异常", handlerMethod, request, response);
    }
		
  	//实际的逻辑处理
  	//HandlerMethod，注入 HandlerMethod 可以拿到当前访问的 mapping 的详细信息，这里直接拿到方法返回类型，进行判断是否是自定义的 JSON 类型，是就返回 JSON，否则返回 ModelAndView
    private Object handle(int code, String msg, HandlerMethod handlerMethod, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = getStatus(request);

        if (handlerMethod.getMethod().getReturnType().getCanonicalName().equals(MyResponse.class.getCanonicalName())) {
            return ResponseEntity.status(status.value()).body(MyResponse.error(code, msg));
        } else {
          	//Spring 默认的错误请求的 URL Mapping 就是 /error，这里自定义一个 error.html
            ModelAndView modelAndView = new ModelAndView("error");

            response.setStatus(status.value());

            modelAndView.addObject("error", msg);
            modelAndView.addObject("status", status.value());
            modelAndView.addObject("code", code);

            return modelAndView;
        }
    }
		
  	//摘自 Spring 源码
    protected HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        try {
            return HttpStatus.valueOf(statusCode);
        } catch (Exception ex) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
```



统一了所有的请求响应，前端处理也就简单多了，假如使用 jQuery Ajax 的话，可以利用全局事件来统一处理所有的异常，如下：

```javascript
$(document).ajaxComplete(function (event, jqxhr, settings) {
				var responseJSON = jqxhr.responseJSON;

        if (jqxhr.status !== 200 || (responseJSON && responseJSON.code !== 0)) {
            //统一的错误处理
            console.error('请求失败');
        } else {
          	//统一的成功处理
            console.error('请求成功');
        }
});
```



## 结论

这样就做到了传统 Web 应用开发当中，当请求处理出现异常的时候，能够根据实际的返回类型来返回期望的值。否则，如果全部返回 JSON，当一个本来是跳转页面的请求发生异常时，浏览器看到的就是白页面了，用户体验不太好，如上即可 dispatch 到 error.html 页面了。保证了请求的正常流转，以及较好的用户体验。

对于 Web 分离的模式，都统一返回 JSON 就简单多了，交给前端自己做处理。

