# RobotKit ![Kotlin Logo](kotlin.png)

RobotKit it's a Kotlin multi-Robot SDK for Android.


## Last version

1.0.0 Release Candidate RC4 : 

``` groovy
    compile 'com.github.ghostwan:robotkit:1.0.0rc4'
```
A [KDOC](https://ghostwan.github.io/robotkit/docs/javadoc/library/index.html) À Kadoc ! [;)](https://media.giphy.com/media/wWSicFanND2gw/200.gif)

## Disclaimer
It could works in java but was not design for it. 

To handle asynchronous calls **RobotKit** use an experimental feature of kotlin calls [coroutine](https://kotlinlang.org/docs/reference/coroutines.html), to use those you need to add in your module build.gradle file :


``` groovy
kotlin {
    experimental {
        coroutines "enable"
    }
}
```

In your activity or where you want to call RobotKit APIs you have to use the lambda : [launch(UI){}](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md#coroutine-basics) or RobotKit shortcut ui{ } or Activity extention inUI{ }

``` kotlin
ui { //it means that the coroutine it's in the UI thread
    pepper.connect() // it's a suspended call
    /* the coroutine in the thread will be suspended
    until the robot is connected but the thread is not blocked.  
    If it fails it will throw an exception.*/
    
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

**Note:** if you want to handle exceptions all at once you can use the helper uiSafe

``` kotlin
uiSafe (onRun = { 
    pepper.connect()
    myTextView.text = "hello world" 
    pepper.say("Hello world")
    pepper.animate(R.raw.elephant_animation)
}, onError = {
    when(it){
        is RobotUnavailableException -> println("Robot unavailble ${it.message}")
        is QiException -> println("Robot Exception ${it.message}")
        is Resources.NotFoundException ->  println("Android resource missing ${it.message}")
        is CancellationException -> println("Execution was stopped")
        else -> it?.printStackTrace() 
    }
})
```

## Compatibility
It works with :

* Pepper (QiSDK) from SoftBank Robotics (https://android.aldebaran.com/sdk/doc/pepper-sdk/index.html)


Future Robot support:

* Nao from SoftBank Robotics 
* Cozmo from Anki (http://cozmosdk.anki.com/docs/)


## Let's code

[KDOC](https://ghostwan.github.io/robotkit/docs/javadoc/library/index.html)

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
        pepper = LocalPepper(this)
        pepper.setOnRobotLost {
            println("Robot Lost : $it")
        }
        textview = findViewById(R.id.text)
    }
    
    override fun onStart() {
        super.onStart()
        ui {
            pepper.connect()
            textview.setText(R.string.hello_world)
            pepper.say(R.string.hello_world)
        }
    }
}
```

To deploy on a virtual Pepper refers to [SoftBank Robotics Documentation](https://android.aldebaran.com/sdk/doc/pepper-sdk/getting_started.html)

## API DONE

### Connect to Pepper attached to the tablet

[Pepper KDOC](https://ghostwan.github.io/robotkit/docs/javadoc/library/com.ghostwan.robotkit.robot.pepper/-pepper/index.html)

``` kotlin
pepper = LocalPepper(this@MainActivity)
pepper.setOnRobotLost {
    println("Robot Lost : $it")
}
if(pepper.isConnected())
    pepper.connect()
```

### Say a phrase

[Say KDOC](https://ghostwan.github.io/robotkit/docs/javadoc/library/com.ghostwan.robotkit.robot.pepper/-pepper/say.html)

Make the robot say something

Simple API:

``` kotlin
pepper.say(R.string.hello_world)
```
        
Say a phrase and make a special animation

``` kotlin
pepper.say(R.string.hello_world, R.raw.elephant_animation)
```

Say and display "hello world" in French, while the tablet is in English :

``` kotlin
pepper.say(R.string.hello_world, locale = Locale.FRENCH)

...

textview.setText(R.string.hello_world, locale = Locale.FRENCH)
```


    
### Animate pepper with animation

[Animate KDOC](https://ghostwan.github.io/robotkit/docs/javadoc/library/com.ghostwan.robotkit.robot.pepper/-pepper/animate.html)

Make the robot run an animation.

Simple API:

``` kotlin
pepper.animate(R.raw.elephant_animation)
```


### Listen concepts

[Listen KDOC](https://ghostwan.github.io/robotkit/docs/javadoc/library/com.ghostwan.robotkit.robot.pepper/-pepper/listen.html)

Make the robot listen for a phrase or group of phrase known as concept

``` kotlin
val helloConcept = Concept(this, R.string.hello, R.string.hi)
val byeConcept = Concept(this, R.string.bye, R.string.see_you)
val concept = pepper.listen(helloConcept, byeConcept)
when (concept) {
    helloConcept -> pepper.say(R.string.hello_world)
    byeConcept -> pepper.say(R.string.bye_world)
    else -> pepper.say(R.string.i_dont_understood)
}
```

Listen in french, it will use the proper localized french ressources for each concepts:

``` kotlin
val concept = pepper.listen(helloConcept, byeConcept, discussConcept, locale = Locale.FRENCH)
```

### Discuss about something

[Discuss KDOC](https://ghostwan.github.io/robotkit/docs/javadoc/library/com.ghostwan.robotkit.robot.pepper/-pepper/discuss.html)

Simple API:

Start a discussion

``` kotlin
val result : String = pepper.discuss(R.raw.cooking_dicussion)
```
    
Start a discussion and go to bookmark "intro"

``` kotlin
val result : String = pepper.discuss(R.raw.cooking_dicussion, gotoBookmark = "intro")
```    

Start a discussion in french and go to bookmark "intro"

``` kotlin
val result = pepper.discuss(R.raw.presentation_discussion, gotoBookmark = "intro", locale = Locale.FRENCH)
```   

Expert API:

``` kotlin
val discussion = Discussion(R.raw.cooking_dicussion)
//val discussion = Discussion(this, R.raw.cooking_dicussion, locale = Locale.FRENCH)

discussion.restoreData(this)
pepper.discuss(discussion)
    
    
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

### Allow multiples task in parallel
``` kotlin
val t1 = uiAsync { pepper.say("Nous voilà dans la cuisine!") }
val t2 = uiAsync { pepper.animate(R.raw.exclamation_both_hands_a003) }
val t3 = uiAsync { nao.animate(R.raw.exclamation_both_hands_a003) }

println("Task are done ${t1.await()} ${t2.await()} ${t3.await()}")
```

### Stop what pepper is doing

[Stop KDOC](https://ghostwan.github.io/robotkit/docs/javadoc/library/com.ghostwan.robotkit.robot.pepper/-pepper/stop.html)

Stop all actions :

``` kotlin
pepper.stop()
```

Or a specific action :

``` kotlin
pepper.stop(Action.SPEAKING);
...
pepper.stop(Action.LISTENING);
...
pepper.stop(Action.MOVING);
```


### Say something when clicking a button

``` kotlin
val myButton = findViewById<Button>(R.id.my_button)
myButton.setOnClickCoroutine { 
    pepper.say(R.string.hello_world)
}
...

val myButton = findViewById<Button>(R.id.my_button)
myButton.setOnClickSafeCoroutine({
    pepper.say(R.string.hello_world)
}, this::onError)


fun onError(throwable : Throwable?) {
    val exceptionMessage = throwable.message
    val message = when (throwable) {
        is QiException -> "Robot Exception $exceptionMessage"
        is RobotUnavailableException -> "Robot unavailable $exceptionMessage"
        is Resources.NotFoundException -> "Android resource missing $exceptionMessage"
        is CancellationException -> "Execution was stopped"
        else -> exceptionMessage
    }
    if (it !is CancellationException && it != null)
        exception(it, "onError")
    message?.let {
        Snackbar.make(rootLayout, message, Snackbar.LENGTH_LONG).show()
    }
}
```

*** 
***
*** 
    
    
## API DOING

### Multiple Robot support #27

Creation of an interface Robot which will be implemented by all Robot supported

``` kotlin 
pepper : Robot = RemotePepper(this,"pepper.local")
pepper : Robot = LocalPepper(this)
nao : Robot = RemoteNao(this,"nao.local")
cozmo : Robot = RemoteCozmo(this, "cozmo.local")
```    



### Connect to Remote Nao #14

``` kotlin
nao = RemoteNao(this, "nao.local")
nao.connect()
```

### Connect to Remote Pepper #13

``` kotlin
pepper = RemotePepper(this, "pepper.local")

//pepper = RemotePepper(this, "192.168.1.23")
pepper.connect()    
```

### Detect touch #23

``` kotlin
pepper.setOnBodyTouched {
    when(it) {
        Body.HEAD -> pepper.say("My head was touched")
        Body.RIGHT_HAND -> pepper.say("My right hand was touched")
        Body.LEFT_HAND -> pepper.say("My left hand was touched")
    }
}
```

*** 
***
*** 
    
    
## API TODO


    
### Connect to Remote Cozmo

``` kotlin
cozmo = RemoteCozmo(this, "cozmo.local")
cozmo.connect()
```

### Animate with animation #15

Expert API:

``` kotlin
val animation = Animation(R.raw.dog_a001)
animation.getDuration()
animation.getLabels()
pepper.animate(animation)
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
nao.follow(pepper)
pepper.follow(cozmo)
```
    
An API for a Robot to follow another robot


### Remember something

``` kotlin
pepper.remember("discussion:result", result)
pepper.remember("discussion:state", state)
nao.remember("discussion:result", result)
```    
    
### Wait for a human

``` kotlin
val human : Human = pepper.waitForHuman()
```
    
### Engage a human

``` kotlin
pepper.engage(human)
```    

### Control robot autonomous abilities 

``` kotlin
pepper.deactivate(Abilitie.BLINKING)
...
pepper.deactivate(Abilitie.BACKGROUND_MOVEMENTS, Abilitie.AWARNESS);
...
pepper.activate(Abilitie.BLINKING, Abilitie.AWARNESS);
...
pepper.activate(Abilitie.BACKGROUND_MOVEMENTS);

```

### Play sound

``` kotlin
pepper.playSound(R.raw.suprise1, R.raw.suprise2, isLooping = true, isRandom = true)
pepper.stopSound()
```

## TO FIX (WIP version on this branch)

- pepper is not disconnected after a discuss
- can't import robotkit snapshot anymore from another project
- don't support ARM 64 processor

