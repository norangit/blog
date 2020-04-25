# SpringBoot Thymeleaf 和 Vuejs 一起使用



传统**非分离**的 **Web** 项目中，**Spring** 官方推荐优先使用 **Thymeleaf** 模板引擎来渲染 **HTML**。为了提升开发效率，在前端可以结合使用 **Vuejs，BootstrapVue** 这 2 个黄金搭档。

关于 **Vuejs** 是一个前端框架，可用实现双向绑定。参考 Vuejs 中文站，有详细的介绍，最适合那些懂点前端知识的后端开发人员了。参考这里：[Vue.js 是什么](https://cn.vuejs.org/v2/guide/index.html#Vue-js-是什么)。

关于 **BootstrapVue** 是一个基于 **Vuejs** 的 **Bootstrap** 增强框架，因为 **Bootstrap** 是很流行的移动端优先的响应式布局框架，当然也提供了一些常用组件，结合 **Vuejs** 使得前端开发更加高效，更加符合语义化，能够自动添加 ARIA 属性，（主要是残疾人士友好的一个规范，因为残疾人士，可能无法操作鼠标，或者无法看得到屏幕，所以需要借助屏幕阅读器来实现，虽说大部分时候都不需要考虑，但是基于人性化，就存在了这么一个规范）[ARIA 是什么戳这里](https://www.w3.org/WAI/standards-guidelines/aria/)。

关于 **Thymeleaf** 是一个后端模板引擎，可以用来生成 HTML，当然也可以生成别的模板化的代码。

## Vuejs 的缺陷

**Vuejs** 虽然很好用，但是它也有一个很大的缺陷，就是不支持在 html 标签里初始化 model 属性，比如：

```javascript
<input type="text" v-model="name" class="form-control" th:value="${backendName}" placeholder="Typein name"/>
```

这个 v-model="name"，只会读取 js 里的 name 变量，忽略默认的 value 属性，当然可以在 js 里给一个默认值，但是有些时候就是想使用后端的默认值，这种情况就只得多写代码来解决了，麻烦。



## Thymeleaf 与 Vuejs 一起使用时典型的常用写法



### Thymeleaf 绑定 Vuejs 属性

在实际使开发中，有时候希望后端直出部分代码，再辅以 Vuejs，比如有一个商品列表，想在后端直出到 html，然后使用 Vuejs 做交互：

```html
<ul class="product-list">
   <li th:each="prod,iterStat: ${prodList}" @click="onProdClicked(prod.id)" :class="{hot: prod.buyTotal > 100}" th:text="${prod.name}"></li>
</ul>
```

在遍历 li 的时候希望直出 li 列表，然后 click 事件、class 属性由 Vuejs 来控制，click 的方法里希望能拿到 id，class 里希望能拿到 buyTotal，甚至还有更多的属性，这个时候就可以使用 Thymeleaf 提供的 **th:attr** 标签来初始化，**Thymeleaf** 规定，如果在一个标签里绑定多个 html 属性的时候需要使用 **th:attr**，不能使用单个绑定，在上面的示例里就是 **th:click、th:class** 这样单个绑定的多个属性，这样会报错。要这样用：

```html
<li th:each="prod,iterStat: ${prodList}" th:attr="':class'=${'{hot: ' + prod.buyTotal + ' > 100}'},'v-on:click'=${'onProdClicked(' + prod.id + ')'}}" th:text="${prod.name}"></li>
```

**th:attr** 的格式是这样的：

```javascript
th:attr="'key1'=${value1}, 'key2'=${'a2' + value2 + 'a3'}", 'key3'=${value3 + 'a3'}"
```

由于 Vuejs 里 html 属性带有冒号:、@ 等符合，不是标准的 html 属性名，所以需要使用引号括起来。



### Thymeleaf 直出文本，不包含标签

有时候只需要输出文本，不想要标签，类似于 Vuejs 里提供的 **<template></template>**，可以使用 **<th:block th:utext="${prod.name}"/>**



### Thymeleaf 生成带参数的 URL，并进行 encode

有时候需要直出带参数或者是动态的 url，通常都是基于 Context 的相对路径，使用@{/path/to/sth}，假如 **Context Path** 是 **/ctx**，则：

> 动态的 url：

```html
<a th:href="@{/a/b/{urlVariable}/c/d(urlVariable=${prod.id})}">
#会生成如下 html，假如 prod.id = 1
<a th:href="/ctx/a/b/1/c/d">  
```



> 带 query 参数的 url：

```html
<a th:href="@{/a/b/c(q1=${prod.id},q2=${prod.name})}">
#会生成如下 html，假如 prod.id = 1，prod.path = '/a/b/c'，会进行 HTML 转义
<a th:href="/ctx/a/b/c?q1=1&amp;q2=prodName">  
```

HTML 转义的好处在于，假如在页面上要查看后台服务的日志文件列表，并提供下载功能，日志文件在硬盘上也是以路径来表示的，如：/var/log/app1/debug.log，c:\var\log\app1\info.log（这是 windows 系统路径，不转义的话会，点击链接的时候后台会报错），/var/log/app1/error.log 等。使用 Thymeleaf 生成一个简单的列表：

```html
<ul>
    <li th:each="log: ${logList}">
        <a th:href="@{/log/download(path=${log.path})}"></a>
    </li>
</ul>
```

> 实际生成的 html：

```html
<ul>
    <li>
        <a href="/ctx/log/download?path=/var/log/app1/debug.log">debug.log</a>
        <a href="/ctx/log/download?path=c:%5Cvar%5Clog%5Capp1%5Cinfo.log">info.log</a>
    </li>
</ul>
```

> 生成绝对路径：

```
<a th:href="@{http://www.thymeleaf/documentation.html}">
#会生成如下 html
<a th:href="http://www.thymeleaf/documentation.html">  
```



> 还可以生成带锚点的 url 等，详细信息：[戳这里](https://www.thymeleaf.org/doc/articles/standardurlsyntax.html)



除了以上这 3 个基本上剩下的都是简单的了，都可以在 **Thymeleaf** 官方文档中找到对应的详细说明。


