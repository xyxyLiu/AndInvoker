# AndInvoker
A tiny Android IPC framework

based on Android Binder, ContentProvider. 

[ ![Download](https://api.bintray.com/packages/tonyreginald/maven/AndInvoker/images/download.svg) ](https://bintray.com/tonyreginald/maven/AndInvoker/_latestVersion)

## Gradle Dependency
```groovy
repositories {
    ...
    jcenter()
}

dependencies {
    implementation 'com.reginald:andinvoker:xxx'
}
````

## Usage

### Register ContentProviders

define AndInvokerProviders for each ipc process
```java
public class ProcessAProvider extends AndInvokerProvider {}
public class ProcessBProvider extends AndInvokerProvider {}
....
````

register them in AndroidManifest.xml
```xml
        <provider
            android:name="ProcessAProvider"
            android:authorities="${applicationId}.process.a"
            android:exported="false"  
            android:process=":a" />
            
        <provider
            android:name="ProcessBProvider"
            android:authorities="${applicationId}.process.b"
            android:exported="false"  
            android:process=":b" />
            
        ....    

````

### Register service (Binder/IInvoker/Interface)

* Register a Binder
```java
// your aidl Binder 
public class MyBinder extends IMyBinder.Stub {
    .......
}

// register a binder service with name in local/remote process
AndInvoker.registerService(context, "provider_authorities", "binder_name", new IServiceFetcher<IBinder>() {
    @Override
    public IBinder onFetchService(Context context) {
        return new MyBinder();
    }
});
````

* Register an Invoker
```java
public class MyInvoker implements IInvoker {
    @Override
    public Bundle onInvoke(Context context, String methodName, Bundle params, ICall callback) {
        // handle invoke here ...
        
        // callback here if needed
        if (callback != null) {
            Bundle data = new Bundle();
            // ...
            callback.onCall(data);
        }
        
        // return result
        return new Bundle();
    }
}

// register invoker in local/remote process
AndInvoker.registerInvoker(context, "provider_authorities", "invoker_name", MyInvoker.class);
````

* Register an interface
interface shared between processes must be annotated with **@RemoteInterface**. 

```java
@RemoteInterface
public interface IMyInterface {
    String testBasicTypes(int i, long l, String s);
    Bundle setCallback(@RemoteInterface IMyCallback callback);
}

// register interface in local/remote process
AndInvoker.registerInterface(context, "provider_authorities", "interface_name", new IMyInterfaceImpl(), IMyInterface.class);
````

register data codec for your custom data type(demo: [GsonCodec](https://github.com/xyxyLiu/AndInvoker/tree/master/demo/src/main/java/com/reginald/andinvoker/demo/gson/GsonCodec.java))
```java
AndInvoker.appendCodec(Class<S> yourCustomClass, Class<R> serializeClass, Codec<S, R> codec);

````

* Supported data types: 
    * all types supported by [android.os.Parcel](https://developer.android.com/reference/android/os/Parcel)
    * [RemoteInterface](https://github.com/xyxyLiu/AndInvoker/tree/master/andinvoker/src/main/java/com/reginald/andinvoker/api/RemoteInterface.java)
    * data types registered with [Codec](https://github.com/xyxyLiu/AndInvoker/tree/master/andinvoker/src/main/java/com/reginald/andinvoker/api/Codec.java)


### Fetch service (Binder/IInvoker/Interface)

* Fetch a Binder
```java
IBinder binderService = AndInvoker.fetchServiceNoThrow(context,
                        "provider_authorities", "binder_name");

IMyBinder myBinder = IMyBinder.Stub.asInterface(binderService);

````

* Invoke an IInvoker
```java
Bundle result = AndInvoker.invokeNoThrow(context, "provider_authorities", "invoker_name","method_name", params, callback)
````

* Fetch an interface
```java
IMyInterface myInterface = AndInvoker.fetchInterfaceNoThrow(context, "provider_authorities", "interface_name", IMyInterface.class);

try {
    myInterface.testBasicTypes(1, 2L, "test");
} catch(InvokeException e) {
    // may throw InvokeException if remote service dies or other remote errors
}

````
