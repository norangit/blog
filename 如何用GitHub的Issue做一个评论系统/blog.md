# 巧用 GitHub 来做一个评论系统

一个评论系统对于任何一个博客，或者说可以提问的站点来说都是很重要的，因为可以互动。如果没有评论系统就好比 { 失去了 }，是不完整的，鸡肋的。但是好多现成的评论系统像 Disqus、Talkyard、Staticman V2（这个和我接下来要说的实现工具一样都是基于 GitHub）、Gitalk 等，优缺点我就不细说了，请参考这篇详细的文章 -> [静态博客评论系统的比较及选择](https://weiweitop.fun/2019/08/10/%E9%9D%99%E6%80%81%E5%8D%9A%E5%AE%A2%E8%AF%84%E8%AE%BA%E7%B3%BB%E7%BB%9F%E7%9A%84%E6%AF%94%E8%BE%83%E5%8F%8A%E9%80%89%E6%8B%A9/)；

接下来是重点，我要使用 GitHub 提供一揽子功能完整的评论系统，所有数据全部安全的保存在 GitHub，既减轻了自己服务器的压力，又减少了硬盘空间的占用，因为个人博客通常都是需要买云服务器的，硬盘老贵了。

## 这个评论系统-他具有以下功能

1. 只需要建立一次 repo 就够了

2. 增删改查全部 API 调用不需要 BackEnd Server 参与，减轻服务器压力

3. 要足够安全，因为 GitHub API 的安全机制，一个 Personal Access Token 可以有很多详细的权限配置，那对于评论系统来说，读写 Repo 是基本的必须要具有的。那就需要考虑安全了，因为这个 Token 在每次访问 GitHub API 的时候都要携带，那如果是 JS 直接调用的话，一个小白都可以拿到这个 Token 了，就可以玩一玩你的评论系统了，增删改查啥都能干。

4. 点赞，踩它，甚至更多，共支持 8 个动作，可以自己参考 GitHub 的 8 个 reaction。

5. 使用 GraphQL 一次抓取全部需要的数据过来，减轻 GitHub 的压力 ^_^，他也不容易啊。

6. 暂时还没想到，回头再补充

接下来开始撸吧，对了关于 GraphQL 的介绍，其实光是使用的话也很简单，主要分 2 点，查询和操作，更准确点应该叫 **读、写**，野鸡：一个 Query，一个 Mutation。就这么简单，更详细的语法请参阅官方网站，是有中文网站的哦 ->[GraphQL](https://graphql.cn/)

### 了解 GitHub 提供的 GraphQL API，戳这里 -> [GraphQL API v4](https://developer.github.com/v4/#overview)

主要了解一下他提供的功能，和限制，对于一个个人博客来说我觉得都不需要在乎，什么5000次访问等，如果你的博客能达到这个限制了，那我有理由相信你足以有能力自己去开发一个更好的评论系统了。
V3 版本是传统的 JSON 格式，不支持一次抓取多种数据。
打开 API 以后，可以简单的看下介绍，主要流程如下：

1. 认证，请参考官方示例创建一个 Personal Access Token
2. EndPoint: [https://api.github.com/graphql](https://api.github.com/graphql)（API 地址，就这一个就够了）
3. 接下来可以可以开始玩了

#### 1. 建立一个 Repository

很容易的，就不说了

#### 2. 使用 QL 创建一个评论，在 GitHub 里叫 Issue Comment

对于评论系统来说每一个 Issue 就是一个博客的一篇文章，一个 Issue（文章） 可以有多个回复，每个回复可以有点赞，踩它，等等各种姿势，最多支持 8 种；

好心的 GitHub 还提供了一个在线版的 QL 编辑器，可以调试好了，直接把 QL 代码 copy 到代码里使用，[戳这里](https://developer.github.com/v4/explorer/)。

1. 创建一个 Issue：

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

2.删除一条评论：

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

3.修改一条评论：

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

4.查询评论：

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
    ignoreCSRF: true,
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

#### 3. 安全问题。因为上面把 Authorization Token 放在了 UI 上，所以有人要做坏事喽

解决这个安全问题稍微有点麻烦了，需要 Nginx 配合了，通常生产环境的部署都会使用 Nginx 做最前端的入口，反向代理也就是。那就可以把这个 Authorization Header 配置到 Nginx location 块里，比如：

```Shell
location / {
    add_header Authorization '';
    return 301 https://example.com$request_uri;
}
```

但是问题又来了，Token 是拿不到了，但是可以给这个 API 发送删除请求啊。。。这尼玛，防不胜防啊 -_-||；
这下这个不仅仅是麻烦了，还有点复杂了，需要用到 Nginx 的一个模块：[lua-nginx-module](https://github.com/openresty/lua-nginx-module)

这个模块的主要功能就是增强了好多 Nginx 本身不具有的功能，比如在一个 proxy_pass 里，我们需要根据请求携带的参数来做出判断，是否需要真正代理请求到真实 Server，这是很有用的，可以减轻被代理 Server 的压力。可以利用 **content_by_lua_block** 来处理，具体可以戳这里 -> [content_by_lua_block 示例](https://github.com/openresty/lua-nginx-module#ngxreqget_post_args)

```Shell
 location = /test {
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
 }
```


