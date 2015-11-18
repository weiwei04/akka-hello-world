/**
 * Created by weiwei on 11/17/15.
 */

package com.baidu;

import akka.actor.*;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HelloAkka {
    public static void main(String[] args) {
        new HelloAkka().run();
    }

    public void run() {
        try {
            // create actor system
            final ActorSystem system = ActorSystem.create("HelloAkka");

            // create greeter actor
            final ActorRef greeter =
                    system.actorOf(Props.create(Greeter.class),
                               "greeter");

            // create inbox
            final Inbox inbox = Inbox.create(system);

            // set greeter param by send message
            greeter.tell(new WhoToGreet("akka"), ActorRef.noSender());

            // send greeter a Greet
            // and we should receive a replay from greeter with
            // message ["hello, akka"]
            inbox.send(greeter, new Greet());

            // expect inbox receive replay from greeter with in 5s
            Greeting greeting =
                    (Greeting) inbox.receive(Duration.create(5, TimeUnit.SECONDS));
            System.out.println("inbox: " + greeting.getMessage());

            greeter.tell(new WhoToGreet("typesafe"), ActorRef.noSender());

            inbox.send(greeter, new Greet());
            greeting =
                    (Greeting)inbox.receive(Duration.create(5, TimeUnit.SECONDS));
            System.out.println("inbox: " + greeting.getMessage());

            // a loop
            final ActorRef greetPrinter =
                    system.actorOf(Props.create(GreetPrinter.class), "GreetPrinter");

            system.scheduler().schedule(Duration.Zero(),
                                        Duration.create(1, TimeUnit.SECONDS),
                                        greeter, new Greet(),
                                        system.dispatcher(),
                                        greetPrinter);
        } catch (TimeoutException e) {
            System.out.println("exception occurred, message[" +
                    e.getMessage() + "]");
        }
    }

    public static class Greet implements Serializable {

    }

    public static class WhoToGreet implements Serializable {
        private final String who;
    
        public WhoToGreet(String who) {
            this.who = who;
        }
    
        String getWho() {
            return who;
        }
    }
    
    public static class Greeting implements Serializable {
        private final String message;
    
        public Greeting(String message) {
            this.message = message;
        }
    
        String getMessage() {
            return message;
        }
    }
    
    public static class Greeter extends UntypedActor {
        private String greeting = "";

        public void onReceive(Object message) {
            if (message instanceof WhoToGreet) {
                greeting = "hello, " +
                        ((WhoToGreet)message).getWho();
            } else if (message instanceof Greet) {
                getSender().tell(new Greeting(greeting),
                        getSelf());
            } else {
                unhandled(message);
            }
        }
    }

    public static class GreetPrinter extends UntypedActor {
        private int count;

        public GreetPrinter() {
            super();
            count = 0;
        }

        public void onReceive(Object message) {
            if (message instanceof Greeting) {
                System.out.println("idx[" + count + "], message[" +
                                   ((Greeting) message).getMessage() + "]");
                ++count;
                getSender().tell(new WhoToGreet("" + count),
                        getSender());
            } else {
                unhandled(message);
            }
        }
    }
}
