# method-logger [![](https://jitpack.io/v/kelvindules/method-logger.svg)](https://jitpack.io/#kelvindules/method-logger)

Method Logger is an annotation based logging library.

## Usage

```java
public class MyClass() {
  @LogMethod
  public Object getObject() {
    // return an object
  }
}
```
Basic log output:
```
MyClass -> getSomething(arg1, arg2) in 1 ms
```
Log output with serialization on:
```
MyClass -> getSomething(arg1, arg2): {
  "field1": "value1"
} in 1 ms
```

## Configuring

TODO
