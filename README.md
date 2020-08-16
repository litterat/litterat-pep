


Litterat:pep (projection-embedded pairs)
------------------

[![GitHub](https://img.shields.io/github/license/litterat/pep-java)](https://github.com/litterat/pep-java/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.litterat/litterat-pep.svg)](https://search.maven.org/search?q=io.litterat.litterat-pep)
[![badge-jdk](https://img.shields.io/badge/jdk-11-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![Follow Litterat](https://img.shields.io/twitter/follow/litterat_io.svg?style=social)](https://twitter.com/litterat_io)
[![Follow Oobles](https://img.shields.io/twitter/follow/oobles.svg?style=social)](https://twitter.com/oobles)


This library is an implementation of some of the ideas written in [Towards Better Serialization](https://github.com/openjdk/amber-docs/blob/master/site/design-notes/towards-better-serialization.md) to create a class descriptor for use in serialization libraries and possibly other purposes. 
It is not a serialization library in itself, but a way of describing how a class should be
serialized. It is based on a discussion on the Java Amber mailing on [embedded-projection pairs](https://mail.openjdk.java.net/pipermail/amber-dev/2020-August/006445.html). 

The library is used to generate a class descriptor which can be used to **project** an object to a Object[]. The Object[]
can then be used by an **embed** function to create the target object type with the values set. Usage of the library can be summed up with the following.


```java
	// Create an instance object to be projected.
	Point p1 = new Point(1,2);
	
	// Create a context and a descriptor for the target class.
	PepContext context = new PepContext();
	PepClassDescriptor pointDescriptor = context.getDescriptor(Point.class);
	
	// Extract the values to an array
	Object[] values = pointDescriptor.project(p1);
	
	// Create the object from the values
	Point p2 = pointDescriptor.embed(values);
```

The Object[] values  can then be used by the serialization library to encode the data
to a stream. The PepClassDescriptor provides meta data for each of the values/fields in the Object[]. This information 
can be used to create the encoding schema for serialized data. The **project** and **embed** functions provided by
the PepClassDescriptor are only sample implementations. A serialization library can use the PepFieldDescriptors directly
in the serialization process to create low-GC overhead implementation.

## Maven dependencies

Library is available from the [Maven repository](https://mvnrepository.com/artifact/io.litterat/litterat-pep) using dependency:

```
<dependency>
  <groupId>io.litterat</groupId>
  <artifactId>litterat-pep</artifactId>
  <version>0.5.0</version>
</dependency>
```

or for Gradle

```
// https://mvnrepository.com/artifact/io.litterat/litterat-pep
implementation group: 'io.litterat', name: 'litterat-pep', version: '0.5.0'
```

## Building

Gradle 6.5 has been used for building the library.


## License

Litterat-pep is available under the Apache 2 License. Please see the LICENSE file for more information.


## Background & Design


The [Towards Better Serialization](https://cr.openjdk.java.net/~briangoetz/amber/serialization.html) document describes many issues
with the existing Java serialization, as well as an interesting new Java language feature to make serialization easier. The aim
of this library is to achieve the same goals without the addition of Java language features. In addition, the serialized form should be
derived from the public API of the class. This is different to the Java serialization libraries which has special capabilities to create object instances with non-existent no-args constructors and write to private final fields. Finally, it should be possible to provide
a way of describing what elements of a class needs to be serialized without resorting to using user defined methods like readObject/writeObject
that effectively encode the schema of serialization in code. By extracting the elements to be serialized from a class the structure
can be encoded and shared with readers of the data. 

The concept of the embedded-projection pairs is being used simplistically as: there is a subset S(small) for any class B(big) that we want to serialize.  We want to define two functions, **project** and **embed** to go to and from S (the structure to serialize) and B (the target object).

	project: B -> S
	embed: S -> B

In this case S is represented by the Object[] and B is represented by the target class. In many cases an intermediate object S will be required
to represent the serialized state prior to creating the Object[]. The general form of the project function is:

```java
	// First project the target to get projected form. May be an identity function.
	Object small = project.invoke(big);
	
	// Get the values from the projected object.
	Object[] values = new Object[fields.length];
	for (int x=0; x<fields.length; x++) {
		values[x] = fields[x].getValue();
	}
	return values;
```

Conversely the embed function can be simplified to the following:

```java
	// Create the object passing in required arguments
	Object small = new Small(values[0],...)
	
	// Call any setters.
	for (int x=0; x<fields.length; x++) {
	    if (fields[x].writeHandle() != null ) {
		    // equivalent to small.setX(values[x]);
			writeHandle.invoke(small, values[x]); 
		}
	}
	
	// call the embed function and return Big.
	return (Big) embed.invoke(small)
```

In some cases it will be a simple one to one mapping between Big and Small with the project/embed functions being an identity function. 
In other cases the difference between the serialized form and original are quite different. In all cases a projected value S should
be equal to project(embed(S)). That is, going from serialized form to target and back should result in the same serialized form. However,
going from target object to serialized form and back to target does not neccessarily result in the same form. The target object
may not project all of its data.

## Under development

The library is currently under heavy development so expect some rough edges. Things still to do include:

 * Versioning - A single target class may have multiple versions and may require multiple project/embed functions.
 * Collections - Collections (List,Map,Set,etc) will likely require some special handling to make them perform well.
 * Interfaces - From a serialization point of view interfaces are similar to a union and will likely require some special handling.

## Reference

#### PepContext

The PepContext provides a library of PepClassDescriptors. Each instance of a PepContext can have one mapping for a particular target
class. This allows different communication streams to define different project/embed functions depending on the context of the
communications. The interface has a simple interface with three functions.

```java
  // get the class descriptor for the target class.
  public <T> PepClassDescriptor<T> getDescriptor(Class<T> targetClass) throws PepException;
  
  // get the class descriptor for the target class using a Project Embed function pair.
  public <T> PepClassDescriptor<T> getDescriptor(Class<T> targetClass, ProjectEmbedPair<T, ?> pePair) throws PepException;

  // get the class descriptor for the target class using a specific Embed implementation.
  public <T> PepClassDescriptor<T> getDescriptor(Class<T> targetClass, Embeds<T> embeds) throws PepException;
```

A PepException will be thrown if a descriptor could not be generated, or if a call to getDescriptor uses a different Embeds/ProjectEmbedPair
implementation than was previously registered.

**NOTE**: This interface may need to be expanded to allow an optional version to be specified. 

### PepClassDescriptor & PepFieldDescriptor

The PepClassDescriptor provides the descriptor of any provided class such that it can be used by a serialization library to serialize
an object instance. A PepClassDescriptor provides the project/embeds method handles and conveniences functions. Both PepClassDescriptor
and PepFieldDescriptor are immutable.

The PepClassDescriptor has the following fields:

```java
	// The class to be projected.
	private final Class<T> targetClass;

	// The embedded class type.
	private final Class<?> embedClass;

	// constructs, calls setters and embeds. Has signature: T embed( Object[] values ).
	private final MethodHandle embedFunction;

	// Calls getters on projected class
	private final MethodHandle getter;

	// Converts from Object[] to targetClass. Has signature: Object[] project( T object );
	private final MethodHandle projectFunction;

	// All fields in the projected class.
	private final PepFieldDescriptor[] fields;
```

The PepFieldDescriptor has the following fields:

```java
	// name of the field
	private final String name;
	
	// type of the field
	private final Class<?> type;
	
	// is the field optional
	private final boolean isOptional;
	
	// set if the field is set in the constructor
	private final int ctorArg;

	// accessor read handle. signature: type t = object.getT();
	private final MethodHandle readHandle;
	
	// setter write handle. Null for constructors. signature: object.setT( type t );
	private final MethodHandle writeHandle;
```

### Projects & Embeds interfaces

The **Projects** interface adds a projects method to a class so that the developer can modify the projected form of the class.
For example, the following Location object projects LocationData converting the format of the data. The LocationData
implements the Embeds interface to return the Location object.


```java
public class Location implements Projects<LocationData> {
   private final int latDeg;
   private final int latMin;
   private final int lonDeg;
   private final int lonMin;

   public static class LocationData implements Embeds<Location> {
      private final float lat;
      private final float lon;
      
      public LocationData( float lat, float lon ) {
      	this.lat = lat;
      	this.lon = lon;
      }
      
      public Location embeds() {
         int latDeg = Math.floor(lat);
         int lonDeg = Math.floor(lon);
      	return new Location( latDeg, Math.floor( (lat-latDeg)*60 ), 
      	                     lonDeg, Math.floor( (lon-lonDeg)*60 )
      }
   }
   
   public Location(int latDeg, int latMin, int lonDeg, int lonMin) {
      this.latDeg = latDeg;
      this.latMin = latMin;
      this.lonDeg = lonDeg;
      this.lonMin = lonMin;
   }
   
   LocationData projects() {
      return new LocationData( latDeg + (lonMin/60), lonDeg + (lonMin/60) );
   }
   
   ... accessors ...
}
```

While the target object Location stores multiple elements, the projected form for serialization uses
LocationData with two float values. 

```java
	// Create an instance object to be projected.
	Location loc1 = new Location(-37,14, 144, 26);
	
	// Build a descriptor
	PepClassDescriptor locationDescriptor = PepClassDescriptor.describe(Location.class);
	
	// Extract the values to an array
	Object[] values = pointDescriptor.project(loc1);
	
	float latitude = values[0];
	float longitude = values[1];
	
	// Create the object from the values
	Location loc2 = (Location) pointDescriptor.embed(values);
```

A class can implement the Projects interface and the projected class doesn't need to implement the **Embeds** interface.
In this case, it must provide a constructor which accepts the projected class. 

```java
public class Location implements Projects<LocationData> {
   private final int latDeg;
   private final int latMin;
   private final int lonDeg;
   private final int lonMin;

   public static class LocationData {
      private final float lat;
      private final float lon;
      
      public LocationData( float lat, float lon ) {
      	this.lat = lat;
      	this.lon = lon;
      }
   }
   
   public Location(int latDeg, int latMin, int lonDeg, int lonMin) {
      this.latDeg = latDeg;
      this.latMin = latMin;
      this.lonDeg = lonDeg;
      this.lonMin = lonMin;
   }
   
   public Location(LocationData data) {
   	  int latDeg = Math.floor(data.lat);
      int lonDeg = Math.floor(data.lon);
      return new Location( latDeg, Math.floor( (data.lat-latDeg)*60 ), 
      	                     lonDeg, Math.floor( (data.lon-lonDeg)*60 )
   }
   
   LocationData projects() {
      return new LocationData( latDeg + (lonMin/60), lonDeg + (lonMin/60) );
   }
   
   ... accessors ...
}
```

A class can provide a class that implements the Embeds interface.
In this case the Embeds class must accepts the target class as a constructor.

Note: It might be worth allowing the descriptor to take a second parameter of the embedded
class type. Also need a better way to link Location to LocationData in this scenario (an annotation on the class?)

```java
public class Location {
   private final int latDeg;
   private final int latMin;
   private final int lonDeg;
   private final int lonMin;

   public static class LocationData implements Embeds<Location> {
      private final float lat;
      private final float lon;
      
      public LocationData( float lat, float lon ) {
      	this.lat = lat;
      	this.lon = lon;
      }
      
      public LocationData( Location location ) {
         this.lat = latDeg + (latMin/60);
         this.lon = lonDeg + (lonMin/60);
      }
      
      public Location embeds() {
         int latDeg = Math.floor(lat);
         int lonDeg = Math.floor(lon);
      	return new Location( latDeg, Math.floor( (lat-latDeg)*60 ), 
      	                     lonDeg, Math.floor( (lon-lonDeg)*60 )
      }
   }
   
   public Location(int latDeg, int latMin, int lonDeg, int lonMin) {
      this.latDeg = latDeg;
      this.latMin = latMin;
      this.lonDeg = lonDeg;
      this.lonMin = lonMin;
   }
   
   ... accessors ...
}
```


### Simple objects

Plain Old Java Objects (POJOs) are serialized based on getter/setter pairs. In this case
a no-argument constructor must be provided.

```java
public class Point {
   private int x;
   private int y;
   
   public Point() {}
   
   public int getX() {
      return x;
   }
   
   public void setX(int x) {
   	  this.x = x;
   }
   
   public int getY() {
      return y;
   }
   
   public void setY(int y) {
      this.y = y;
   }
}
```

In many cases the object being serialized are simple immutable objects like the following. 
There is no projects/embeds implementations. As there is a single constructor it is taken to be the 
structure to use for the serialized form.

```java
public class Point {
   private final int x;
   private final int y;
   
   public Point(int x, int y) {
      this.x = x;
      this.y = y;
   }
   
   public int getX() {
      return x;
   }
   
   public int getY() {
      return y;
   }
}
```

An interesting property of Java reflection is that unless a runtime parameter is added to the command
line, the names of the constructor parameters can not be discovered. As such, it is not possible at runtime
to determine the order of x and y in the Point constructor example above. 

The Pep library uses byte code
analysis to determine the ordering of the constructor parameters. The byte code analysis currently only works for
simple constructor param to field assignments, as such the Field annotation may be required. In the
example below the x and y values are modified which will currently fool the code analysis. As
such the Field annotation has been added.

```java
public class Point {
   private final int x;
   private final int y;
   
   public Point( @PepField("x") int x, @PepField("y") int y) {
      this.x = x*2;
      this.y = y*3;
   }
   
   public int getX() {
      return x;
   }
   
   public int getY() {
      return y;
   }
}
```

It is also possible to mix immutable fields with mutable fields. For instance:

```java
public class Point {
   private final int x;
   private final int y;
   
   private int z;
   
   public Point( int x,  int y) {
      this.x = x;
      this.y = y;
   }
   
   public int getX() {
      return x;
   }
   
   public int getY() {
      return y;
   }
   
   public int getZ() {
   	 return z;
   }
   
   public void setZ(int z) {
      this.z = z;
   }
}
```

The three fields will be serialized with x,y,z values. 



### PepField annotation

The field annotation can be used to override the properties of a field or to assist the library in finding
the constructor/accessor pairs for a given class. In the following example the fields are renamed such
that their projected form descriptor will have the field xx, yy. This can be useful where the serialized
schema uses a different naming schema than Java.

```java
public class Point {
   private final int x;
   private final int y;
   
   public Point(int x, int y) {
      this.x = x;
      this.y = y;
   }
   
   @PepField(name="xx")
   public int getX() {
      return x;
   }
   
   @PepField(name="yy")
   public int getY() {
      return y;
   }
}
```

The field annotation can also be used to specify if a value is optional. By default any field that could
have a null value will be described as being optional. In the following example, the class does not allow
non-null name values, however, the projected field will by default have name as optional true. The
Field annotation has been used to override this and make it false.

```java
public class Name {

   @PepField(optional=false)
   private final String name;
   
   public Name(String name) {
   	   Objects.requiureNonNull(name, "name can not be null");
   	   this.name = name;
   }
   
   public String name() {
   	   return name;
   }
}
```

The field annotation can be added to one of the constructor parameter, accessor, setter or class field. An error
will occur if the annotation has been added to multiple places for the same logical field.

### PepConstructor annotation

The PepConstructor is used to specify which constructor should be used when there are multiple options.

```java
public class Point {
   private final int x;
   private final int y;
   
   @PepConstructor
   public Point(int x, int y) {
      this.x = x;
      this.y = y;
   }
   
   public Point(float x, float y) {
      this.x = (int) x;
      this.y = (int) y;
   }
   
   public int getX() {
      return x;
   }
   
   public int getY() {
      return y;
   }
}
```

It can also be used to specify a constructor on a static method where singletons or object pooling is used. The static constructor
must match the parameters of the constructor for the object.

```java
public class Point {

   private static final Map<Integer, Map<Integer,Point>> xPoints = new HashMap<>();
   
   @PepConstructor
   public static Point get(int x, int y) {
      Map<Integer,Point> yPoints = xPoints.get(x);
      if (yPoints == null) {
      	yPoints= new HashMap<>();
      }
      
      Point p = yPoints.get(y);
      if (p == null) {
      	p = new Point(x,y);
      	yPoints.put(y,p);
      }
      return p;
   }

   private final int x;
   private final int y;
      
   private Point(int x, int y) {
      this.x = x;
      this.y = y;
   }
   
   public Point(float x, float y) {
      this.x = (int) x;
      this.y = (int) y;
   }
   
   public int getX() {
      return x;
   }
   
   public int getY() {
      return y;
   }
}
```

### ProjectEmbedPair interface

The ProjectEmbedPair interface is for creating an external projection and embedding function for the target object.
This can be useful where the target object is for an external library.

```java
 public static class LocationPep implements ProjectEmbedPair<Location,LocationData> {

      public Location embeds(LocationData data) {
         int latDeg = Math.floor(data.latitude());
         int lonDeg = Math.floor(data.longitude());
      	return new Location( latDeg, Math.floor( (data.latitude()-latDeg)*60 ), 
      	                     lonDeg, Math.floor( (data.longitude()-lonDeg)*60 )
      }
   }
   
   LocationData projects(Location) {
      return new LocationData( latDeg + (lonMin/60), lonDeg + (lonMin/60) );
   }
}

public class Location {
   private final int latDeg;
   private final int latMin;
   private final int lonDeg;
   private final int lonMin;

   public Location(int latDeg, int latMin, int lonDeg, int lonMin) {
      this.latDeg = latDeg;
      this.latMin = latMin;
      this.lonDeg = lonDeg;
      this.lonMin = lonMin;
   }
   
   ... accessors ...
}

public class LocationData implements Embeds<Location> {
   private final float lat;
   private final float lon;
  
   public LocationData( float lat, float lon ) {
  	 this.lat = latitude;
  	 this.lon = lon;
   }
   
   ... accessors ...
}   
```

This is used when describing the object. The LocationPep object is passed in as the second parameter to the describe function.
The LocationData object is used as the serialization object.

```java
	// Create an instance object to be projected.
	Location loc1 = new Location(-37,14, 144, 26);
	
	// Build a descriptor
	PepContext context = new PepContext();
	PepClassDescriptor locationDescriptor = context.describe(Location.class, new LocationPep());
	
	// Extract the values to an array
	Object[] values = pointDescriptor.project(loc1);
	
	float latitude = values[0];
	float longitude = values[1];
	
	// Create the object from the values
	Location loc2 = (Location) pointDescriptor.embed(values);
```


## Copyright

Copyright (c) 2020, Live Media Pty. Ltd. All Rights Reserved.
