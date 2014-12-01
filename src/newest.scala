import akka.actor._
import com.typesafe.config.ConfigFactory
import java.security.MessageDigest
import akka.routing.RoundRobinRouter
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import com.typesafe.config.ConfigFactory
import java.net.InetAddress
import scala.util.control.Breaks

sealed trait Bits
case class add_result(input: ArrayBuffer[String]) extends Bits
case class work(message: String) extends Bits
case class r(hex: ArrayBuffer[String]) extends Bits
case class count(start: Int, end: Int, message: String, num_of_zeros: Int) extends Bits
case class print(ans: ArrayBuffer[String]) extends Bits
case class Client_Result(input: ArrayBuffer[String]) extends Bits
case class Initiate(num_of_zeros: Int,message: String) extends Bits


//master object
object newest {
  def main(args: Array[String]) {
    println("master")

    val hostname = InetAddress.getLocalHost.getHostName
    val config = ConfigFactory.parseString(
      """ 
     akka{ 
    		actor{ 
    			provider = "akka.remote.RemoteActorRefProvider" 
    		} 
    		remote{ 
                enabled-transports = ["akka.remote.netty.tcp"] 
            netty.tcp{ 
    			hostname = "192.168.0.22" 
    			port = 3553
    		} 
      }      
    }""")
    implicit val system = ActorSystem("HelloRemoteSystem", ConfigFactory.load(config))
    
    var message = "sdabadi"
    var workers_count = 2
    var message_count = 100
    var num_of_zeros = args(0).toInt
    var Final : ArrayBuffer[String]=new ArrayBuffer[String]
    var inputs : Int=0
    val listener = system.actorOf(Props(new listener(Final,inputs)), name = "listener")
    val Remote_Actor = system.actorOf(Props(new Remote_Actor(num_of_zeros,message,listener)), name = "Remote_Actor")
    val master = system.actorOf(Props(new master(num_of_zeros, workers_count, message, message_count, listener)), name = "master")
  }
  
  //Actor which is called by Client
  class Remote_Actor(num_of_zeros: Int,message: String,listener: ActorRef) extends Actor {   
    def receive = {
        case "Request_Work" => {
        println("request received")
        sender ! Initiate (num_of_zeros,message) //send to the client
      }
      case Client_Result(input) => {
       listener ! add_result(input)
      }
      case _ => {
        println("Unknown Message Received .")
      }
    }
  }

  
  //For calculating bitcoins
  class worker extends Actor {

    def receive = {
      case count(start: Int, end: Int, message: String, num_of_zeros: Int) => generateBitCoins(start, end, message, num_of_zeros)
    }
    
    def generateBitCoins(start: Int, end: Int, message: String,num_of_zeros:Int) = {
      var result: ArrayBuffer[String] = new ArrayBuffer[String]();
      for (j <- 1 to 100000) {
        for (i <- start to end) {
          val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
          val sb = new StringBuilder
          for(j<-1 to i )
          {
            val randomNum = util.Random.nextInt(chars.length)
            sb.append(chars(randomNum))
          }
          var s: String = sb.toString
          hashMap( message +s, result,num_of_zeros)
        }
      }
      sender ! r(result)
    }
    
    def hashMap(s: String, result: ArrayBuffer[String], num_of_zeros: Int) = {
      val mdbytes: Array[Byte] = MessageDigest.getInstance("sha-256").digest(s.getBytes())
      val hexString: StringBuffer = new StringBuffer();
      for (i <- 0 to mdbytes.length - 1) {
        var str: String = Integer.toHexString(0xFF & mdbytes(i));
        if (str.length() == 1) str = "0" + str;
        hexString.append(str);
      }
      var search_key: String = ""
      for (i <- 0 until num_of_zeros)
        search_key += "0";
      if (hexString.toString.startsWith(search_key))
      {
        result+=s
        result += (hexString.toString)
      }
    }
  }
  class listener(Final : ArrayBuffer[String],inputs : Int) extends Actor {
   
    def receive = {
      case add_result(input)=>{
        if(input.length!=0)
        {
        	println("bitcoins from client:");
          for (i <- 0 until input.length by 2) {
            println(input(i) + "   :  " + input(i + 1))
          }
        }
      }
      case print(ans) => {
        if (ans.length != 0) {
        	println("bitcoins by server:");
          for (i <- 0 until ans.length by 2) {
            println(ans(i) + "   :  " + ans(i + 1))
          }
        }
       // context.system.shutdown()        
      }      
    }
  }
  
  //gets called when we create the master actor
  class master(num_of_zeros: Int, workers_count: Int, message: String, message_count: Int, listener: ActorRef) extends Actor {
    var num = 0: Int
    var result: ArrayBuffer[String] = new ArrayBuffer[String]();
    val worker = context.actorOf(
      Props[worker].withRouter(RoundRobinRouter(workers_count)), name = "worker")//Schedules the actors on a roundrobin basis
      var a =10 :Int
    for (i <- 1 to message_count){ 
      worker ! count(a, a+1, message, num_of_zeros)
        a +=2 
      if (a==20) a=10
    }
    def receive = {
      case r(ans) =>
        { result ++= (ans); num += 1 }
        if (num == message_count) 
          listener ! print(result)
    }
  }
}