# RobotKit
RobotKit it's a Kotlin multi-Robot SDK for Android.

It works with :
* Pepper (QiSDK) from SoftBank Robotics (https://android.aldebaran.com/sdk/doc/pepper-sdk/index.html)

Future Robot support:
* Nao from SoftBank Robotics 
* Cozmo from Anki (http://cozmosdk.anki.com/docs/)


## API DONE

### Connect to Pepper attached to the tablet

    pepper = MyPepper(this@MainActivity)
    pepper.setRobotLostListener {
        println("Robot Lost : $it")
    }
    pepper.connect()

### Say a phrase [DONE]

Make the robot say something

Simple API:

    pepper.say(R.string.hello_world)    

### Listen concepts [DONE]

Make the robot listen for a phrase or group of phrase known as concept

    val helloConcept = Concept(this@MainActivity, R.string.hello, R.string.hi)
    val byeConcept = Concept(this@MainActivity, R.string.bye, R.string.see_you)
    val concept = pepper.listen(helloConcept, byeConcept)
    when (concept) {
        helloConcept -> pepper.say(R.string.hello_world)
        byeConcept -> pepper.say(R.string.bye_world)
        else -> pepper.say(R.string.i_dont_understood)
    }
    
## API TODO

### Multiple Robot support

Creation of an interface Robot which will be implemented by all Robot supported
 
    pepper : Robot = RemotePepper(this,"pepper.local")
    myPepper : Robot = MyPepper(this)
    nao : Robot = RemoteNao(this,"nao.local")
    cozmo : Robot = RemoteCozmo(this, "cozmo.local")


### Connect to Remote Pepper

    pepper = RemotePepper(this, "pepper.local")
    pepper.connect()
    
### Connect to Remote Nao

    nao = RemoteNao(this, "nao.local")
    nao.connect()
    
### Connect to Remote Cozmo

    cozmo = RemoteCozmo(this, "cozmo.local")
    cozmo.connect()

### Say a phrase

Expert API:

    val speech = Speech(R.string.hello_world)
    pepper.say(speech)

### Animate with animation

Make the robot run an animation.

Simple API:

    pepper.animate(R.raw.elephant_animation)

Expert API:

    val animation = Animation(R.raw.dog_a001)
    animation.getDuration()
    animation.getLabels()
    myPepper.animate(animation)

### Move around

    pepper.moveForward(1)
    pepper.moveLeft(1)
    pepper.moveRight(1)
    
### Goto a location

     val theKitchen : Location = pepper.getLocation("kitchen")
     pepper.goTo(theKitchen)

Where Location is struct that represent a position in the robot world

    val theKitchen = nao.getLocation("kitchen")
    pepper.rememberLocation("kitchen", theKitchen)
    
Allow multiple robots to share a location that they know about
    

### Follow a robot

    nao.follow(myPepper)
    myPepper.follow(cozmo)
    
An API for a Robot to follow another robot


### Discuss about something

Simple API:

    val result : String= myPepper.discuss(R.raw.cooking_dicussion);

Expert API:

    val discussion = Discussion(R.raw.cooking_dicussion)
    myPepper.discuss(discussion);
    
### Remember something

    myPepper.remember("discussion:result", result)
    nao.remember("discussion:result", result)
    
### Wait for a human

    val human : Human = myPepper.waitForHuman()
    
### Engage a human

    myPepper.engage(human)

### Allow multiples task in parallel

    val t1 : Task= () -> myPepper.say("Nous voilà dans la cuisine!");
    val t2 : Task = () -> myPepper.animate(R.raw.exclamation_both_hands_a003);
    val t3 : Task = () -> nao.animate(R.raw.exclamation_both_hands_a003);

    Tasks.Parallele(t1, t2).execute();

### Control robot autonomous abilities 

    myPepper.deactivate(Robot.AUTONOMOUS_BLINKING);


