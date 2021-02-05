# pooled-data-source

#### 介绍
通用连接池 可以实现任意的连接池

#### 软件架构
data-source-core 通用连接池架构 => 已经编写好的连接池模板 直接继承使用
sql-data-source-boot-start sql类数据库 的连接池 整合spring boot 也算个经典实例，看看使用data-source-core 实现连接池有多简单
ftp-data-source FTP连接池 整合spring boot 
##### data-source-core 
ConcurrentBorrowBag implements BlockingQueue 
里面存着连接池带 取出和放入 连接只是修改连接池带的元素状态 没有的话会异步添加连接 
DataSourceConfig 
通用连接池配置 需要实现详细的连接池配置 如sql连接池配置
GeneralPool 
通用池 少数模板方法有待实现 主要存在为异步和调度 添加空闲连接 回收超过的连接 回收超时连接
##### sql-data-source-boot-start 
具体的sql 连接池实现
        


