# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**

>I see you are using Panache `PanacheEntity` and `PanacheStore`.  
They both support same operations, so that relly depends on your project structure. How complex business logic in the project.  
And how complex retrieval operations in the DB layer. Do you need multiple data access layers (for ex. NoSql as primary and SQL as a fallback)?
Do you have really complex analytic queries?
For example here's my quarkus project (My old Blog) with Quarkus native AWS Lambda with AWS Codepipeline CI/CD pipeline.  
I decided to use DynamoDB as a primary DB. Using such DB as source of truth is not an easy task.  
[Here's a DB model for AWS NoSQL Workbench](https://github.com/srcmaxim/aws-blog/blob/master/src/blog-lambda/Blog%20Data%20Model.json)
> <details>
>  <summary>Click to see DB model</summary>
>
>| **PK**                      | **SK**               | **Facet**   | **Title**                   | **Content**                      | **ReadMinutes** | **PublishDate** | **Message**                           | **UserEmail**              | **GSI1PK**                     | **GSI1SK**                |
>|-----------------------------|----------------------|-------------|------------------------------|----------------------------------|-----------------|----------------|----------------------------------------|-----------------------------|--------------------------------|---------------------------|
>| `POST#build-dynamodb-blog`  | `POST`               | `Post`      | `Building a DynamoDB Blog`   | `Introduction to DynamoDB...`    | `5`             | `2024-08-21`   | -                                      | -                           | `TAG#dynamodb`                | `2024-08-21`              |
>| `POST#build-dynamodb-blog`  | `TAG#0`              | `Tag`       | `DynamoDB`                   | -                                | -               | -              | -                                      | -                           | `POST#build-dynamodb-blog`    | `DynamoDB`                |
>| `POST#build-dynamodb-blog`  | `TAG#1`              | `Tag`       | `AWS`                        | -                                | -               | -              | -                                      | -                           | `POST#build-dynamodb-blog`    | `AWS`                     |
>| `POST#build-dynamodb-blog`  | `MSG#2024-08-21`     | `Message`   | -                            | -                                | -               | -              | `Great post! Learned a lot!`           | `user1@example.com`         | `POST#build-dynamodb-blog`    | `MSG#2024-08-21`          |
>| `POST#build-dynamodb-blog`  | `MSG#2024-08-22`     | `Message`   | -                            | -                                | -               | -              | `Thanks for the details!`              | `user2@example.com`         | `POST#build-dynamodb-blog`    | `MSG#2024-08-22`          |
>| `POST#introduction-aws`     | `POST`               | `Post`      | `Introduction to AWS`        | `AWS is a cloud platform...`     | `7`             | `2024-08-19`   | -                                      | -                           | `TAG#aws`                     | `2024-08-19`              |
>| `POST#introduction-aws`     | `TAG#0`              | `Tag`       | `Cloud`                      | -                                | -               | -              | -                                      | -                           | `POST#introduction-aws`       | `Cloud`                   |
>| `POST#introduction-aws`     | `TAG#1`              | `Tag`       | `AWS`                        | -                                | -               | -              | -                                      | -                           | `POST#introduction-aws`       | `AWS`                     |
>| `POST#introduction-aws`     | `MSG#2024-08-20`     | `Message`   | -                            | -                                | -               | -              | `Interesting read!`                    | `user3@example.com`         | `POST#introduction-aws`       | `MSG#2024-08-20`          |
>
></details>
>
> Working with such DB efficiently is quite complex. For example to get all data (post, tags, messages) for 1 post you need to set `partitionKey=POST#build-dynamodb-blog` ans `sortKey=*`. Then of course you need to construct your aggregate of the Post yourself.  
Repository approach is better for [such tasks](https://github.com/srcmaxim/aws-blog/blob/master/src/blog-lambda/src/main/java/io/srcmaxim/blog/repository/DynamoDbRepository.java).  
And ActiveRecord is better for really simple queries (for example blog on Postgres DB).
>
>Great project structure BTW. Good approach to hexagonal architecture.
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
> `Warehouse API` is already existing service that you guys keep in sync with a new service.  
Since this API is already implemented (`Warehouse API`) you're using OpenAPI spec to keep new APIs in sync with old API.  
For new API Product and Store you're generating specifications from code since it's convenient.  
When one service wants to mirror API of other service, or we want to generate Client from server API we need to use existing API.  
Then we can test new service and old service or client and server with Packt contract testing.
Also we can run 2 queries to old and new service and compare them (shadow rollout). In IKEA you should do it at least 1 year :)
> 
> Also as I see from my implementation of StoreResource, my code guarantees `at most once` [1-0] delivery guarantee.  
We can archive `at least once`/`exactly once` if we use [Transaction Outbox pattern](https://microservices.io/patterns/data/transactional-outbox.html).
Some things that we should also think of is having `versionId`, `traceId`, `createdAt` stored in the old system to ensure we don't have race conditions and the event is traceable.  
Also we might have problems with `exactly once` delivery of the messages, so we need to check `idempodencyKey` in the old system.
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
> I would use [The Code Review Pyramid](https://www.morling.dev/blog/the-code-review-pyramid) and concentrate on unit tests first.  
Then I would write IT tests since they're really easy to implement in this setup.
I generated OpenAPI file for testing `./mvnw quarkus:dev` and `curl http://localhost:8080/q/openapi` and added it to Postman.
You can also use http://localhost:8080/q/swagger-ui/. For me it was easier to spot business logic bugs in such way!
>
> I'd follow [this blog posts](https://blog.sebastian-daschner.com/tags/testing) these posts.  
> - Thoughts on efficient enterprise testing (1/6)  
> - Efficient enterprise testing — unit & use case tests (2/6)  
> - Efficient enterprise testing — integration tests (3/6)  
> - Efficient enterprise testing — workflows & code quality (4/6)  
> - Efficient enterprise testing — test frameworks (5/6)  
> - Efficient enterprise testing — conclusion (6/6)  
>
> [And this one is really good too](https://google.github.io/building-secure-and-reliable-systems/raw/ch13.html).
> 
> For good coverage you need to setup your SonarQube profiles/project heath with Alerts when code coverage of new code goes down.
You can also add code coverage check before you merge (and skip if you really need it).
>
> Also you need to test for DB invariants with load testing.
Example of this is `phantom read problem` during Warehouse creation.
See more details in `CreateWarehouseUseCase.java:51`.
