# RobotKit ![Kotlin Logo](kotlin.png)

RobotKit it's a Kotlin multi-Robot SDK for Android.

## Last version

1.0.0 Release Candidate RC2 : 

``` groovy
    compile 'com.github.ghostwan:robotkit:1.0.0rc2'
```
A [KDOC](https://jitpack.io/com/github/ghostwan/robotkit/1.0.0rc2/javadoc/library/index.html) ;)

## Disclaimer
It could works in java but was not design for it. 
See https://github.com/ghostwan/robotkit-java for the java version 

To handle asynchronous calls **RobotKit** use an experimental feature of kotlin calls [coroutine](https://kotlinlang.org/docs/reference/coroutines.html), to use those you need to add in your module build.gradle file :

``` groovy
kotlin {
    experimental {
        coroutines "enable"
    }
}
```

In your activity or where you want to call RobotKit APIs you have to use the lambda : [launch(UI){}](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md#coroutine-basics) or RobotKit shortcut ui{}

``` kotlin
ui { //it means that the coroutine it's in the UI thread

    if(!pepper.isConnected()) {
        pepper.connect() // it's a suspended call
        /* the coroutine in the thread will be suspended
        until the robot is connected but the thread is not blocked.  
        If it fails it will throw an exception.*/
    }
    
    // then it will update the text view 
    myTextView.text = "hello world" 
    // then it will make peppers says hello world
    pepper.say("Hello world") // it's a suspend call
    // then it will play an elephant animation
    pepper.animate(R.raw.elephant_animation) // it's a suspend call
}
```

without coroutines we would have to use callbacks and it would look like this:

``` kotlin
pepper.connect(onResult = {
    if(it is Success) {
        runOnUiThread { 
            myTextView.text = "hello world"                      
        }
        pepper.say("Hello world", onResult = {
        if(it is Success) 
            pepper.animate(R.raw.elephant_animation)
        })
    }	
})

```

or futures 

``` kotlin
pepper.connect().thenConsume {
    if(it.isSuccess()) {
        runOnUiThread { 
            myTextView.text = "hello world"                      
        }
    }	
}
.thenCompose {
    if(it.isSuccess()) {
        return pepper.say("Hello world")
    }		
}
.thenCompose {
    if(it.isSuccess()) {
        return pepper.animate(R.raw.elephant_animation)
    }		
}
```

## Compatibility
It works with :

* Pepper (QiSDK) from SoftBank Robotics (https://android.aldebaran.com/sdk/doc/pepper-sdk/index.html)


Future Robot support:

* Nao from SoftBank Robotics 
* Cozmo from Anki (http://cozmosdk.anki.com/docs/)


## Let's code

[KDOC SNAPSHOT](https://jitpack.io/com/github/ghostwan/robotkit/-SNAPSHOT/javadoc/library/index.html)

Here the list of [all robotkit version on jitpack](https://jitpack.io/#ghostwan/robotkit)

If you want to test the last SNAPSHOT version in your app,
add in your root build.gradle file :

``` groovy
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" } // To retrieve RobotKit
        maven { url 'https://android.aldebaran.com/sdk/maven'} // For Pepper SDK
        maven { url "https://kotlin.bintray.com/kotlinx" } // For Kotlin Serialization API
        ...
    }
}
```
And in your app build.gradle file:

``` groovy
dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
    ...
    compile 'com.github.ghostwan:robotkit:-SNAPSHOT'
}
kotlin {
    experimental {
        coroutines "enable"
    }
}
```

Create a empty activity and make an hello world
 
``` kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var pepper: Pepper
    private lateinit var textview: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pepper = MyPepper(this)
        pepper.setOnRobotLost {
            println("Robot Lost : $it")
        }
        textview = findViewById(R.id.text)
    }
    
    override fun onStart() {
        super.onStart()
        ui {
            if (!pepper.isConnected()) {
                pepper.connect()
            }
            try {
                textview.setText(R.string.hello_world)
                pepper.say(R.string.hello_world)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
```

To deploy on a virtual Pepper refers to [SoftBank Robotics Documentation](https://android.aldebaran.com/sdk/doc/pepper-sdk/getting_started.html)

## API DONE

### Connect to Pepper attached to the tablet

``` kotlin
pepper = MyPepper(this@MainActivity)
pepper.setOnRobotLost {
    println("Robot Lost : $it")
}
pepper.connect()
```

### Say a phrase

Make the robot say something

Simple API:

``` kotlin
pepper.say(R.string.hello_world)
```
        
Say a phrase and make a special animation

``` kotlin
pepper.say(R.string.hello_world, R.raw.elephant_animation)
```
    
### Animate with animation

Make the robot run an animation.

Simple API:

``` kotlin
pepper.animate(R.raw.elephant_animation)
```


### Listen concepts

Make the robot listen for a phrase or group of phrase known as concept

``` kotlin
val helloConcept = Concept(this@MainActivity, R.string.hello, R.string.hi)
val byeConcept = Concept(this@MainActivity, R.string.bye, R.string.see_you)
val concept = pepper.listen(helloConcept, byeConcept)
when (concept) {
    helloConcept -> pepper.say(R.string.hello_world)
    byeConcept -> pepper.say(R.string.bye_world)
    else -> pepper.say(R.string.i_dont_understood)
}
```

### Discuss about something

Simple API:

Start a discussion

``` kotlin
val result : String = myPepper.discuss(R.raw.cooking_dicussion)
```
    
Start a discussion and go to bookmark

``` kotlin
val result : String = myPepper.discuss(R.raw.cooking_dicussion, gotoBookmark = "intro")
```    

Expert API:

``` kotlin
val discussion = Discussion(R.raw.cooking_dicussion)
discussion.restoreData(this)
myPepper.discuss(discussion)
    
    
...
    
discussion.saveData(this)
    
...
    
discussion.setVariable("name", "ghostwan")
discussion.getVariable("name")
discussion.setOnVariableChanged { name, value -> 
    info("Variable $name changed to $value") 
}
    
    
...
    
discussion.setOnBookmarkReached {
    println("bookmark $it reached!")
}
discussion.gotoBookmark("mcdo")
    
```
    
***
***
*** 
    
    
## API TODO

### Multiple Robot support #27

Creation of an interface Robot which will be implemented by all Robot supported

``` kotlin 
pepper : Robot = RemotePepper(this,"pepper.local")
myPepper : Robot = MyPepper(this)
nao : Robot = RemoteNao(this,"nao.local")
cozmo : Robot = RemoteCozmo(this, "cozmo.local")
```    


### Connect to Remote Pepper #13

``` kotlin
pepper = RemotePepper(this, "pepper.local")
pepper.connect()    
```	

### Connect to Remote Nao #14

``` kotlin
nao = RemoteNao(this, "nao.local")
nao.connect()
```
    
### Connect to Remote Cozmo

``` kotlin
cozmo = RemoteCozmo(this, "cozmo.local")
cozmo.connect()
```

### Say a phrase #28

Expert API:

``` kotlin
val speech = Speech(R.string.intro, R.string.presentation, Locale.FRENCH)
pepper.say(speech)
``` 

### Animate with animation #15

Expert API:

``` kotlin
val animation = Animation(R.raw.dog_a001)
animation.getDuration()
animation.getLabels()
myPepper.animate(animation)
```

### Move around #16

``` kotlin
pepper.moveForward(1)
pepper.moveLeft(1)
pepper.moveRight(1)
```
    
### Goto a location #17

``` kotlin
 val theKitchen : Location = pepper.getLocation("kitchen")
 pepper.goTo(theKitchen)
```

Where Location is struct that represent a position in the robot world

``` kotlin
val theKitchen = nao.getLocation("kitchen")
pepper.rememberLocation("kitchen", theKitchen)
```
  
Allow multiple robots to share a location that they know about #18
    

### Follow a robot #20

``` kotlin
nao.follow(myPepper)
myPepper.follow(cozmo)
```
    
An API for a Robot to follow another robot


### Remember something

``` kotlin
myPepper.remember("discussion:result", result)
myPepper.remember("discussion:state", state)
nao.remember("discussion:result", result)
```    
    
### Wait for a human

``` kotlin
val human : Human = myPepper.waitForHuman()
```
    
### Engage a human

``` kotlin
myPepper.engage(human)
```
    
### Detect touch

``` kotlin
pepper.setOnBodyTouched {
    when(it) {
        Body.HEAD -> pepper.say("My head was touched")
        Body.RIGHT_HAND -> pepper.say("My right hand was touched")
        Body.LEFT_HAND -> pepper.say("My left hand was touched")
    }
}
```

### Allow multiples task in parallel #25

``` kotlin
val t1 : Task= () -> myPepper.say("Nous voilà dans la cuisine!");
val t2 : Task = () -> myPepper.animate(R.raw.exclamation_both_hands_a003);
val t3 : Task = () -> nao.animate(R.raw.exclamation_both_hands_a003);

Tasks.Parallele(t1, t2).execute();
```

### Control robot autonomous abilities 

``` kotlin
myPepper.deactivate(Robot.AUTONOMOUS_BLINKING);
```


