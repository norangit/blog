# CentOS 7 安装 Nginx, Brotli, PageSpeed，提升网页性能

**CentOS 7** 编译安装 **Nginx**，包含以下模块：高性能 **Brotli**，**PageSpeed**。**Brotli** 由 **Google** 出品，经过大量实践测试，性能优于 gzip。压缩体积相比于 gzip：JS 小14%，HTML 小21%，CSS 小17%，效果相当不错。再辅以 **Apache PageSpeed** 模块能大幅提升页面加载速度，让网页飞一会。接下来一言不合，撸起袖子，一起撸啊撸 ...

### 准备工作：

在 **CentOS** 系统里新建一个目录，比如：

```shell
mkdir -p /user/gxfcba
```

下载 **Nginx** 1.17.9 源码，Brotli 源码，PageSpeed 1.13.35.2-stable 源码



1. 下载 **Nginx**

```shell
cd /user/gxfcba
wget http://nginx.org/download/nginx-1.17.9.tar.gz 
tar -zxvf nginx-1.17.9.tar.gz 
```



2. 下载 **Brotli**，如果没有 **Git** 的话需要先装 **Git**

```shell
yum install git (如果没有则安装)
cd /user/gxfcba
git clone https://github.com/google/ngx_brotli.git
```



3. 下载 **PageSpeed 1.13.35.2-stable**， [戳这参考官方安装步骤](https://www.modpagespeed.com/doc/build_ngx_pagespeed_from_source)

```shell
sudo yum install gcc-c++ pcre-devel zlib-devel make unzip libuuid-devel (安装依赖)
#创建个脚本 download-pagespeed.sh，内容为：

#[check the release notes for the latest version]
NPS_VERSION=1.13.35.2-stable
cd
wget https://github.com/apache/incubator-pagespeed-ngx/archive/v${NPS_VERSION}.zip
unzip v${NPS_VERSION}.zip
nps_dir=$(find . -name "*pagespeed-ngx-${NPS_VERSION}" -type d)
cd "$nps_dir"
NPS_RELEASE_NUMBER=${NPS_VERSION/beta/}
NPS_RELEASE_NUMBER=${NPS_VERSION/stable/}
psol_url=https://dl.google.com/dl/page-speed/psol/${NPS_RELEASE_NUMBER}.tar.gz
[ -e scripts/format_binary_url.sh ] && psol_url=$(scripts/format_binary_url.sh PSOL_BINARY_URL)
wget ${psol_url}
tar -xzvf $(basename ${psol_url})

#然后给权限，执行脚本下载，会自动下载到 /root/incubator-pagespeed-ngx-1.13.35.2-stable 目录，如果找不到的话，find 一下
find / -name pagespeed*
```



4. 为了获得 **Nginx** 的编译参数，首先安装一次 **Nginx**，我通常都会打开至少 2 个 ssh 窗口，甚至更多，免得上来下去翻命令，此时就可以新开一个 ssh 窗口，执行别的，等会回来

```shell
yum install nginx
#安装完毕后，输入 
nginx -V
#得到已有的编译参数，后面要用到
```



### 开始编译 Nginx，Brotli，PageSpeed

```shell
cd /user/gxfcba/nginx-1.17.9/
#拷贝刚才 nginx -V 的输出参数，最后加上 Brotli、PageSpeed 的参数，执行：

./configure --prefix=/etc/nginx --sbin-path=/usr/sbin/nginx --modules-path=/usr/lib64/nginx/modules --conf-path=/etc/nginx/nginx.conf --error-log-path=/var/log/nginx/error.log --http-log-path=/var/log/nginx/access.log --pid-path=/var/run/nginx.pid --lock-path=/var/run/nginx.lock --http-client-body-temp-path=/var/cache/nginx/client_temp --http-proxy-temp-path=/var/cache/nginx/proxy_temp --http-fastcgi-temp-path=/var/cache/nginx/fastcgi_temp --http-uwsgi-temp-path=/var/cache/nginx/uwsgi_temp --http-scgi-temp-path=/var/cache/nginx/scgi_temp --user=nginx --group=nginx --with-compat --with-file-aio --with-threads --with-http_addition_module --with-http_auth_request_module --with-http_dav_module --with-http_flv_module --with-http_gunzip_module --with-http_gzip_static_module --with-http_mp4_module --with-http_random_index_module --with-http_realip_module --with-http_secure_link_module --with-http_slice_module --with-http_ssl_module --with-http_stub_status_module --with-http_sub_module --with-http_v2_module --with-mail --with-mail_ssl_module --with-stream --with-stream_realip_module --with-stream_ssl_module --with-stream_ssl_preread_module --with-cc-opt='-O2 -g -pipe -Wall -Wp,-D_FORTIFY_SOURCE=2 -fexceptions -fstack-protector-strong --param=ssp-buffer-size=4 -grecord-gcc-switches -m64 -mtune=generic -fPIC' --with-ld-opt='-Wl,-z,relro -Wl,-z,now -pie' --add-module=/user/gxfcba/ngx_brotli --add-module=/root/incubator-pagespeed-ngx-1.13.35.2-stable

#这个时候应该会提示 Git clone 的是少了一个东西，按照命令行给出的提示执行:

cd /user/gxfcba/ngx_brotli && git submodule update --init && cd /user/gxfcba/nginx-1.17.9

#下载完了，再执行一次上面的 configure 命令，然后开始 make
make && make install
#安装完毕后，确认 Nginx 版本
nginx -v
#启动 Nginx
nginx
#不报错的话启动成功，打开浏览器访问 localhost 能看到 Welcome to nginx! 页面就成功了
```



### 设置开机启动服务

   ```shell
vim /etc/systemd/system/nginx.service
#插入如下：
[Unit]
Description=nginx - high performance web server
Documentation=https://nginx.org/en/docs/
After=network-online.target remote-fs.target nss-lookup.target
Wants=network-online.target

[Service]
Type=forking
PIDFile=/var/run/nginx.pid
ExecStartPre=/usr/sbin/nginx -t -c /etc/nginx/nginx.conf
ExecStart=/usr/sbin/nginx -c /etc/nginx/nginx.conf
ExecReload=/bin/kill -s HUP $MAINPID
ExecStop=/bin/kill -s TERM $MAINPID

[Install]
WantedBy=multi-user.target
   ```



### 推荐一个构建 nginx.conf 文件的网站，只要按照顺序一步步来，基本就是一个优化的配置: [NGINX Config](https://www.digitalocean.com/community/tools/nginx#)



跟着步骤一步步走完后，会得到一个压缩包，按照文档放到 /etc 下的对应目录，我嫌麻烦直接手工 copy 到单个文件里了。然后会看到下面的配置出现 - **brotli 压缩**

```properties
# brotli
brotli on;
brotli_comp_level 6;
brotli_types text/plain text/css text/xml application/json application/javascript application/rss+xml application/atom+xml image/svg+xml;
```



试着点[这里](https://gxfcba.com/test/)访问下测试页面，打开 F12 看看该请求的响应 header，包含了以下信息：

```properties
content-encoding: br
```

这就就说明启用了。但是有个问题就是，对于 proxy_pass 的请求来说，gzip、brotli 会具体情况具体对待，如下：

```html
Syntax: 	gzip_proxied off | expired | no-cache | no-store | private | no_last_modified | no_etag | auth | any ...;
Default: 	gzip_proxied off;
Context: 	http, server, location

off
    disables compression for all proxied requests, ignoring other parameters; 
expired
    enables compression if a response header includes the “Expires” field with a value that disables caching; 
no-cache
    enables compression if a response header includes the “Cache-Control” field with the “no-cache” parameter; 
no-store
    enables compression if a response header includes the “Cache-Control” field with the “no-store” parameter; 
private
    enables compression if a response header includes the “Cache-Control” field with the “private” parameter; 
no_last_modified
    enables compression if a response header does not include the “Last-Modified” field; 
no_etag
    enables compression if a response header does not include the “ETag” field; 
auth
    enables compression if a request header includes the “Authorization” field; 
any
    enables compression for all proxied requests. 
```



本站使用的是 **h2**，而且后台是 **Springboot**，启用了 **Srpingboot** 自带的静态文件 hash 功能，所以即使静态文件（js、css等）并没有被压缩，但是 **Springboot** 会添加 **cache-control: max-age=86400** ，还有**etag**，以及**last-modified** 响应头，此时**gzip_proxied：any**，**gzip**，**brotli** 也不会启用压缩，尽管 http 响应请求里能看到响应头。所以对于 proxy_pass 这种请求来说要么 backend 自己负责压缩，要么不要添加缓存控制的相关 header。



#### 关于 PageSpeed 的使用将会另开一篇来说说，其实一般优化 web，主要就是合并文件，比如合并 css，js，行内化一些小的 js，css，懒加载图片，使用 LocalStorage 等。
