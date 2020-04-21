# 巧用、借用 GitHub 来做一个评论系统



一个评论系统对于任何一个博客，或者说可以提问的站点来说都是很重要的，因为可以互动。如果没有评论系统就好比 { 失去了 }，是不完整的，鸡肋的。但是好多现成的评论系统像 Disqus、Talkyard、Staticman V2（这个和我接下来要说的实现工具一样都是基于 GitHub）、Gitalk 等都不适合，所以决定自己使用 GitHub V4 API 自己搞一个。上述评论系统的优缺点我就不细说了，请参考这篇详细的文章 -> [静态博客评论系统的比较及选择](https://weiweitop.fun/2019/08/10/%E9%9D%99%E6%80%81%E5%8D%9A%E5%AE%A2%E8%AF%84%E8%AE%BA%E7%B3%BB%E7%BB%9F%E7%9A%84%E6%AF%94%E8%BE%83%E5%8F%8A%E9%80%89%E6%8B%A9/)；

接下来是重点，我要使用 GitHub 提供一揽子功能完整的评论系统，所有数据全部安全的保存在 GitHub，既减轻了自己服务器的压力，又减少了硬盘空间的占用，因为个人博客通常都是需要买云服务器的，硬盘老贵了。



## 这个评论系统-他具有以下功能

1. 只需要建立一次 repo 就够了

2. 增删改查全部 API 调用不需要 BackEnd Server 参与，减轻服务器压力

3. 要足够安全，因为 GitHub API 的安全机制，一个 **Personal Access Token** 可以有很多详细的权限配置，那对于评论系统来说，读写 Repo 是基本的必须要具有的。那就需要考虑安全了，因为这个 Token 在每次访问 GitHub API 的时候都要携带，那如果是 JS 直接调用的话，一个小白都可以拿到这个 Token 了，就可以玩一玩你的评论系统了，增删改查啥都能干。

4. 点赞，踩它，甚至更多 6 种姿势，共支持 8 个动作，可以自己参考 GitHub 的 8 个 reaction。

5. 使用 GraphQL 一次抓取全部需要的数据过来，减轻 GitHub 的压力 ^_^，他也不容易啊。

6. 暂时还没想到，回头再补充

接下来开始撸吧，对了关于 **GraphQL** 的介绍，其实光是使用的话也很简单，主要分 2 点，查询和操作，更准确点应该叫 **读、写**，也即：一个 **Query**，一个 **Mutation**。就这么简单，更详细的语法请参阅官方网站，是有中文网站的哦 ->[GraphQL中文站](https://graphql.cn/)



### 了解 GitHub 提供的 GraphQL API，戳这里 -> [GraphQL API v4](https://developer.github.com/v4/#overview)

主要了解一下他提供的功能，和限制，对于一个个人博客来说我觉得都不需要在乎，什么5000次访问等，如果你的博客能达到这个限制了，那我有理由相信你足以有能力自己去开发一个更好的评论系统了。
V3 版本是传统的 JSON 格式，不支持一次抓取多种数据。
打开 API 以后，可以简单的看下介绍，主要流程如下：

1. 认证，请参考官方示例创建一个 **Personal Access Token**
2. EndPoint: [https://api.github.com/graphql](https://api.github.com/graphql)（API 地址，就这一个就够了）
3. 接下来可以可以开始玩了



#### 1. 建立一个 Repository

很容易的，就不说了



#### 2. 使用 QL 创建一个评论，在 GitHub 里叫 Issue Comment

对于评论系统来说每一个 Issue 就是一个博客的一篇文章，一个 Issue（文章） 可以有多个回复，每个回复可以有点赞，踩它，等等各种姿势，最多支持 8 种；

好心的 GitHub 还提供了一个在线版的 QL 编辑器，可以调试好了，直接把 QL 代码 copy 到代码里使用，[戳这里](https://developer.github.com/v4/explorer/)。

1. 创建一个 **Issue**：

``` JavaScript
    # assigneeIds： 指定给谁，根据需要自己指定，理论上来说是评论系统应当指定给自己，注意这是第一次创建 Issue（评论），在 GitHub 里这等于 2 个操作，创建一个 Issue，并且发布一个评论。
    #labelIds：是 Bug 还是别的，请参考官方 API，对于评论来说这个没有也无所谓了。。。
    #repositoryId：顾名思义，Repository ID
    #title: 这个是 Issue 的 title，可以使用博客的标题

    #以下代码是在 repositoryId 为 MDEwOlJlcG9zaXRvcnkxNTU3MTc2NDA= 的仓库下，创建一个 Label 为 MDU6TGFiZWwxMTEyNjU2ODc0 ，标题为 ${title} 的 Issue，注意这里使用到了 ES6 里的字符串模板，反引号
    {
        query: `mutation {
                    createIssue(input: {assigneeIds: null, labelIds: "MDU6TGFiZWwxMTEyNjU2ODc0", projectIds: null, repositoryId: "MDEwOlJlcG9zaXRvcnkxNTU3MTc2NDA=", title: "${title}" }){
                        issue{
                            id
                            number
                        }
                    }
                }`
    }
```

可以在 GitHub 的 Issue 里看看都有哪些 Label，然后挨个加一个，在上面的编辑器里查询刚刚加的 Label，我在 V4 版本里没有找到说明 -_-||：

```JavaScript
{
  repository(name: "你的 Repo 名称", owner: "你的 GitHub 登录名，就你经常能看到的头像右边的那个东西") {
    issue(number: 10) {
      labels(first: 10) {
        edges {
          node {
            id
            name
          }
        }
      }
    }
  }
}

#会返这样的东东，这个 node 下面的 id 就是在创建 Issue 的时候用到的那个 Label：

{
  "data": {
    "repository": {
      "issue": {
        "labels": {
          "edges": [
            {
              "node": {
                "id": "MDU6TGFiZWwxMTEyNjU2ODc0",
                "name": "question"
              }
            }
          ]
        }
      }
    }
  }
}
```

执行完毕后，可以去刷新 GitHub 页面实时看到效果。

2.删除一条评论 **IssueComment**：

```JavaScript
# id：即要删除的评论 id，后面会给出如何拿到
{
    query: `mutation {
                deleteIssueComment(input: {id: "${id}"}){
                    clientMutationId
                }
            }`
}
```

3.修改一条评论 **IssueComment**：

```JavaScript
#和删除评论类似，多了一个 body 参数
{
    query: `mutation {
                updateIssueComment(input: {
                        id: "${id}",
                        body: '新评论内容'
                    }){
                    clientMutationId,
                    issueComment
                }
            }`
}
```

4.查询评论 **IssueComment**：

```JavaScript
#first:, last: ，聪明的你一定会想到这是分页喽，没错就是分页。想查多少条指定多少即可，So easy！

{
  repository(name: "你的 Repo 名称", owner: "你的 GitHub 登录名，就你经常能看到的头像右边的那个东西") {
    issues(last: 10) {
      edges {
        node {
          id
          title
          comments(first: 10) {
            nodes {
              bodyText
              id
            }
          }
        }
      }
    }
  }
}
```
到此为止，增删改查已经都实现完了，下面给出一个使用 jQuery 的示例：

```JavaScript
$.ajax({
    headers: {
        #这里的 xxxxxxxxx 即你的个人访问 Token，
        Authorization: 'token xxxxxxxxx',
    },
    method: "POST",
    url: 'https://api.github.com/graphql',
    data: JSON.stringify({
        query: `mutation {
                    deleteIssueComment(input: {id: "${id}"}){
                        clientMutationId
                    }
                }`
    })
});
```

以上 4 个 API 调用全部都是在 JS 里做的，不需要 BackEnd Server 参与。第 2 点功能也已经实现了。



#### 3. 安全问题。因为上面把 Authorization Token 放在了 UI 上，所以有人可能要做坏事喽

解决这个安全问题稍微有点麻烦了，需要 Nginx 配合了，通常生产环境的部署都会使用 Nginx 做最前端的入口，反向代理也就是。那就可以把这个 Authorization Header 配置到 Nginx location 块里，比如：

```Shell
location /api/comments/ {
		#代理到 GitHub
		proxy_pass https://api.github.com/;
		# options 请求处理
    if ( $request_method = 'OPTIONS' ) {
    	return 200;
    }
    # xxxxxxxx 是 Personal Access Token
    proxy_set_header Authorization "token xxxxxxxx";
}
```

客户端 JavaScript 代码改成如下：

```javascript
$.ajax({
    method: "POST",
    url: 'https://gxfaba.com/api/comments/graphql',
    data: JSON.stringify({
        query: `mutation {
                    deleteIssueComment(input: {id: "${id}"}){
                        clientMutationId
                    }
                }`
    })
});
```

这样就解决了 **Personal Access Token** 被泄露的安全。

但是问题又来了，Token 是拿不到了，但是可以给这个 API 发送删除请求啊。。。这尼玛，防不胜防啊 -_-||；
这下这个不仅仅是麻烦了，还有点复杂了，需要用到 Nginx 的一个模块：[lua-nginx-module](https://github.com/openresty/lua-nginx-module)

这个模块的主要功能就是增强了好多 Nginx 本身不具有的功能，比如在一个 proxy_pass 里，我们需要根据请求携带的参数来做出判断，是否需要真正代理请求到真实的 Server，这是很有用的，可以减轻被代理 Server 的压力。可以利用 **rewrite_by_lua_block** 来处理，具体可以戳这里 -> [rewrite_by_lua_block 示例](https://github.com/openresty/lua-nginx-module#ngxreqget_post_args)

```Shell
 location = /api/comments/ {
     rewrite_by_lua_block {
        ngx.req.read_body()  -- explicitly read the req body
        local data = ngx.req.get_body_data()

        -- 判断请求参数里如果包含有 delete 字样的，拒绝请求
        if string.match(data, 'delete') then
            -- 返回的错误信息
            ngx.say("warning: illegal request...")
            return
        end

        -- body may get buffered in a temp file:
        local file = ngx.req.get_body_file()
        if string.match(file, 'delete') then
            -- 返回的错误信息
            ngx.say("warning: illegal request...")
        else
            ngx.say("no body found")
        end
    }
    
    #代理到 GitHub
		proxy_pass https://api.github.com/;
		# options 请求处理
    if ( $request_method = 'OPTIONS' ) {
    	return 200;
    }
 }
```

当然，如果是管理员要删除的话，这个 URL 是不开放的，只有管理员知道的，就可以不做这个限制了，比如多加一个专门用来删除评论的 location 配置：

```
# reverse proxy
location /api/comments/del/ {
  proxy_pass https://api.github.com/;
  proxy_http_version	1.1;
  proxy_cache_bypass	$http_upgrade;
  proxy_set_header Authorization		"token XXXXXXXXX";
  proxy_connect_timeout      60;
  proxy_send_timeout         60;
  proxy_read_timeout         60;
  access_log off;
}
```

客户端 JavaScript 代码改成如下：

```javascript
$.ajax({
    method: "POST",
    url: 'https://gxfaba.com/api/comments/del/graphql',
    data: JSON.stringify({
        query: `mutation {
                    deleteIssueComment(input: {id: "${id}"}){
                        clientMutationId
                    }
                }`
    })
});
```



关于 Nginx 内部请求处理的几个阶段，可以参考这篇文章了解了解，类似于 Java 里的拦截器链，Vuejs 里的生命周期。戳这里：[淘宝的介绍](https://tengine.taobao.org/book/chapter_12.html#id8)，或者这里：[OpenResty-基于 Nginx 的高性能 Web Server, 据其官网 2012 年给出的性能测试对比，比 Nginx 快了好几倍](https://openresty.org/download/agentzh-nginx-tutorials-zhcn.html#02-NginxDirectiveExecOrder01)。在这里不作过多解释，总的指导原则就是，尽可能早的进行拦截，提升性能。



#### 4.点赞，踩它，其它姿势

在 GitHub 里这个叫 **Pick your reaction**，就是点那个 🌞 表情，会出来 8 个动作。直接看代码：

全面的查询示例：

```javascript
{
  repository(name: "你的 Repo 名称", owner: "你的 GitHub 登录名，就你经常能看到的头像右边的那个东西") 
    issues(last: 30) {
      nodes {
        id
        bodyText
        reactions(first: 10) {
          edges {
            node {
              id
              content
            }
          }
        }
        bodyText
        comments(first: 100) {
          nodes {
            id
            bodyText
            reactions(first: 10) {
              edges {
                node {
                  id
                  content
                }
              }
            }
          }
        }
      }
    }
  }
}

```

如果有的话，就会输出类似如下的结果：

````javascript
{
  "data": {
    "repository": {
      "issues": {
        "nodes": [
          {
            "reactions": {
              "edges": [
                {
                  "node": {
                    "id": "MDg6UmVhY3Rpb242ODIwODUzNQ==",
                    "content": "THUMBS_UP"
                  }
                },
                {
                  "node": {
                    "id": "MDg6UmVhY3Rpb242ODIwODU0Mw==",
                    "content": "HEART"
                  }
                },
                {
                  "node": {
                    "id": "MDg6UmVhY3Rpb242ODIwODU1NA==",
                    "content": "EYES"
                  }
                }
              ]
            },
            "bodyText": "First Comment with reactions...",
            "comments": {
              "nodes": [
                {
                  "bodyText": "写的不错，不过现在 web 开发基本都前后分离了吧",
                  "reactions": {
                    "edges": [
                      {
                        "node": {
                          "id": "MDg6UmVhY3Rpb242ODIwODI0OQ==",
                          "content": "THUMBS_UP"
                        }
                      },
                      {
                        "node": {
                          "id": "MDg6UmVhY3Rpb242ODIwODI3MQ==",
                          "content": "LAUGH"
                        }
                      },
                      {
                        "node": {
                          "id": "MDg6UmVhY3Rpb242ODIwODI5NQ==",
                          "content": "HOORAY"
                        }
                      },
                      {
                        "node": {
                          "id": "MDg6UmVhY3Rpb242ODIwODMxMg==",
                          "content": "EYES"
                        }
                      },
                      {
                        "node": {
                          "id": "MDg6UmVhY3Rpb242ODIwODMzOQ==",
                          "content": "THUMBS_DOWN"
                        }
                      },
                      {
                        "node": {
                          "id": "MDg6UmVhY3Rpb242ODIwODM0OA==",
                          "content": "CONFUSED"
                        }
                      }
                    ]
                  }
                }
              ]
            }
          },
          {
            "reactions": {
              "edges": []
            },
            "bodyText": "",
            "comments": {
              "nodes": [
                {
                  "bodyText": "fsdfsdf",
                  "reactions": {
                    "edges": []
                  }
                },
                {
                  "bodyText": "新评论内容",
                  "reactions": {
                    "edges": []
                  }
                }
              ]
            }
          }
        ]
      }
    }
  }
}
````

JavaScript 代码如下：

````javascript
#THUMBS_UP 向上的大拇指，点赞。subjectId：就是 Issue 或者 Comment 的 ID，参考上面全面的示例部分，在查询的时候把 ID 记得查回来
#删除的方法是 removeReaction
$.ajax({
    method: "POST
    url: 'https://gxfaba.com/api/comments/graphql',
    data: JSON.stringify({
        query: `mutation {
                    addReaction(input: {subjectId: "${id}", content: 'THUMBS_UP'}){
                        clientMutationId
                    }
                }`
    })
});
````

删除的方法是 removeReaction，戳这里 -> [removeReaction](https://developer.github.com/v4/mutation/removereaction/)



#### 5.使用 GraphQL 一次抓取全部需要的数据过来，其实上面已经给出来了，参考上面 - 全面的示例部分。

需要什么，就在查询 QL 里一次性都加上，这样只需要一次就把想要的信息都查回来了，后续修改删除直接拿对应的 Id 当输入参数即可。这就是 GraphQL 的魅力所在。

再加以优化一下，每篇文章的评论列表只加载一次，因为删除权限只有管理员有，修改其实也很鸡肋，可以考虑禁止评论者修改直接的评论。这样的话只加载一次，然后把结果使用 LocalStorage 缓存在浏览器端，下次访问的时候能快速渲染到 UI 上。



##### 目前唯一的问题在于国内到 GitHub 的速度还是比较慢的，尽管国内也有 GitHub 的服务器，但是速度还是比较慢，甚至会出现请求超时失败，考虑到用户体验的话，还是自己建表实现一个评论好点，当然了，不介意速度的话，就用 GitHub 的好喽，省时、省力、省空间，o(*￣︶￣*)o。

前端其实也很简单，对于一个后端开发或者全栈开发来说，根本没必要分离式开发，本站采用的就是 SpringBoot + Thymeleaf + Vuejs + BootstrapVue，可以把 Vuejs 当作升级版的 jQuery 用，还是很好用的。而且，一个博客访问量只要不是每天都有几万的话，性能根本不是问题，如果你是土豪，不差钱，完全可以升级云服务器，加大内存，然后代码里上 Redis 缓存。不然的话，有那么大访问量赚点服务器的钱，挂几个广告应该就够了吧。。。



#### 6.暂未想到还有啥，想到了再来补充。。。

