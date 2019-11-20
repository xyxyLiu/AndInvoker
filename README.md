# AndInvoker
a tiny android ipc framework

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

AndInvoker is based on Android Binder, ContentProvider. 

### Define and register ContentProviders

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

### Register your service

* Register a Binder
```java
// your aidl Binder 
public class MyBinder extends IMyBinder.Stub {
    .......
}

// register a binder service with name in local process
        AndInvoker.registerService("binder_name", new IServiceFetcher() {
            @Override
            public IBinder onFetchService(Context context) {
                return new MyBinder();
            }
        });
````

* Register an Invoker in local process
```java
public class MyInvoker implements IInvoker {
    @Override
    public Bundle onInvoke(Context context, String methodName, Bundle params, ICall callback) {
        // write you code here ...
        // ...
        
        // callback here if you need one
        if (callback != null) {
            Bundle data = new Bundle();
            // ...
            callback.onCall(data);
        }
        
        // return 
        return new Bundle();
    }
}

// register invoker in local process
AndInvoker.registerInvoker("invoker_name", MyInvoker.class);
````

* Register a interface in local process
interface shared between process must be annotated with @RemoteInterface. 

```java
@RemoteInterface
public interface IMyInterface {
    String testBasicTypes(int i, long l, String s);
    Bundle setCallback(@RemoteInterface IMyCallback callback);
}

// register interface in local process
AndInvoker.registerInterface("interface_name", new IMyInterfaceImpl(), IMyInterface.class);
````

register data codec for your custom data type(demo: [GsonCodec](https://github.com/xyxyLiu/AndInvoker/tree/master/demo/src/main/java/com/reginald/andinvoker/demo/gson/GsonCodec.java))
```java
AndInvoker.appendCodec(Class<S> yourCustomClass, Class<R> serializeClass, Codec<S, R> codec);

````

* Supported data types: 
    * all types supported by [android.os.Parcel](https://developer.android.com/reference/android/os/Parcel)
    * [RemoteInterface](https://github.com/xyxyLiu/AndInvoker/tree/master/andinvoker/src/main/java/com/reginald/andinvoker/api/RemoteInterface.java)
    * data types registered with [Codec](https://github.com/xyxyLiu/AndInvoker/tree/master/andinvoker/src/main/java/com/reginald/andinvoker/api/Codec.java)

* Register service to remote process
```java
// register binder in remote process
AndInvoker.registerRemoteService(context, "remote_provider_authorities", "binder_name", new IMyInterfaceImpl(), IMyInterface.class);

// register IInvoker in remote process
AndInvoker.registerInvoker(context, "remote_provider_authorities", "invoker_name", MyInvoker.class);

// register interface in remote process
AndInvoker.registerInterface(context, "remote_provider_authorities", "interface_name", new IMyInterfaceImpl(), IMyInterface.class);
````

### Fetch service

* Fetch a remote Binder
```java
IBinder binderService = AndInvoker.fetchServiceNoThrow(context,
                        "remote_provider_authorities", "binder_name");

IMyBinder myBinder = IMyBinder.Stub.asInterface(binderService);

````

* Invoke a remote IInvoker
```java
Bundle result = AndInvoker.invokeNoThrow(context, "remote_provider_authorities", "invoker_name","method_name", params, callback)
````

* Fetch a remote interface
```java
IMyInterface myInterface = AndInvoker.fetchInterfaceNoThrow(context, "remote_provider_authorities", "interface_name", IMyInterface.class);

myInterface.testBasicTypes(1, 2L, "test");
````
