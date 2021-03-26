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
[L] MyClass -> getObject() in 1 ms
```
Log output with serialization on:
```
[L] MyClass -> getObject() in 1 ms
[R] MyClass -> getObject(): { "field1": "value1" }
```

## Configuring

TODO
