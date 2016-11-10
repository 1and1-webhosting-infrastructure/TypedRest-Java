# TypedRest

TypedRest helps you build type-safe fluent-style JSON REST API clients.

Maven artifacts:
* `com.oneandone:typedrest-annotations`
* `com.oneandone:typedrest-core`
* `com.oneandone:typedrest-vaadin`
* `com.oneandone:typedrest-archetype`


## Nomenclature

We use the following terms in the library and documentation:
* An __entity__ is a data transfer object that can be serialized as JSON.
* An __endpoint__ is a REST resource at a specific URI.
* An __entry endpoint__ is an _endpoint_ that is the top-level URI of a REST interface.
* An __element endpoint__ is an _endpoint_ that represents a single _entity_.
* A __collection endpoint__ is an _endpoint_ that represents a collection of _entities_ and provides an _element endpoint_ for each of them.
* A __trigger endpoint__ is an _endpoint_ that represents an RPC call to trigger a single action (intentionally un-RESTful).


## Usecase sample

We'll use this simple POJO (Plain old Java object) class modelling software packages as a sample _entity_ type:
```java
class PackageEntity {
  private int id;
  @Id public int getId() { return id; }
  public void setId(int id) { this.id = id; }

  private String name;
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
}
```


## Getting started

Include this in your Maven ```pom.xml``` to use the library:
```xml
<dependency>
  <groupId>com.oneandone</groupId>
  <artifactId>typedrest-core</artifactId>
  <version>0.28.1</version>
</dependency>
```

You can then use the classes `EntryEndpoint`, `CollectionEndpointImpl`, `ElementEndpointImpl`, `PollingEndpointImpl`, `ActionEndpointImpl`, `PaginationEndpointImpl`, `StreamEndpointImpl` and `BlobEndpointImpl` to build a local representation of a remote REST service. Based on our usecase sample this could look like this:
```java
class SampleEntryEndpoint extends EntryEndpoint {
  public SampleEntryEndpoint(URI uri) {
    super(uri);
  }

  public CollectionEndpoint<PackageEntity> getPackages() {
    return new CollectionEndpointImpl<>(this, "packages", PackageEntity.class);
  }
}
```

You can then perform CRUD operations like this:
```java
SampleEntryEndpoint server = new SampleEntryEndpoint(URI.create("http://myservice/api/"));
List<PackageEntity> packages = server.packages.readAll();
ElementEndpoint<PackageEntity> element = server.packages.create(new PackageEntity(...));
PackageEntity pack = server.packages.get(1).read();
server.Packages.get(1).update(pack);
server.Packages.get(1).delete();
```


## Build GUI clients

Include this in your Maven ```pom.xml``` to build GUIs with [Vaadin](https://vaadin.com/):
```xml
<dependency>
  <groupId>com.oneandone</groupId>
  <artifactId>typedrest-vaadin</artifactId>
  <version>0.27</version>
</dependency>
```
