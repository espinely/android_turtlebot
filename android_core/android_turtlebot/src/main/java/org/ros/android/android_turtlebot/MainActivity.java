/*
    Android for Turtlebot
    Authors: Yamid Espinel, Pankaj Bagga
    For the Robotic's Engineering II course
    Bachelor in Computer Vision and Robotics
    Université de Bourgogne, France
 */

package org.ros.android.android_turtlebot;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.CameraControlLayer;
import org.ros.android.view.visualization.layer.LaserScanLayer;
import org.ros.android.view.visualization.layer.OccupancyGridLayer;
import org.ros.android.view.visualization.layer.PathLayer;
import org.ros.android.view.visualization.layer.PoseSubscriberLayer;
import org.ros.android.view.visualization.layer.RobotLayer;
import org.ros.android.view.RosImageView;

import java.net.URI;
import java.net.URISyntaxException;

import geometry_msgs.Twist;

import com.zerokol.views.JoystickView;
import com.zerokol.views.JoystickView.OnJoystickMoveListener;


//The following class publish twist messages into the /cmd_vel topic
class Talker extends AbstractNodeMain {

    public double xVel,yVel,zVel; //Variables that contain linear velocities
    public double xAng,yAng,zAng; //Variables that contain angular velocities
    public boolean message_enable=false; //Flag to enable/disable the publishing of twist messages and avoid interference with the path following routine
    private Twist velCommand; //Message of type twist that will contain the velocities to be published


    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("rosjava_android_turtlebot/talker"); //Node identifier
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        final Publisher<geometry_msgs.Twist> pub =
                connectedNode.newPublisher("cmd_vel", geometry_msgs.Twist._TYPE); //Publisher definition with its topic name and the type of message
        // This CancellableLoop will be canceled automatically when the node shuts
        // down.
        connectedNode.executeCancellableLoop(new CancellableLoop() {

            @Override
            //Initialise all the variables:
            protected void setup() {
                xVel = 0;
                yVel = 0;
                zVel = 0;
                xAng = 0;
                yAng = 0;
                zAng = 0;
            }

            @Override
            //Main execution loop:
            protected void loop() throws InterruptedException {

                if(message_enable) {
                    velCommand = pub.newMessage(); //Create new twist message
                    velCommand.getLinear().setX(xVel); //Set linear velocities
                    velCommand.getLinear().setY(yVel);
                    velCommand.getLinear().setZ(zVel);
                    velCommand.getAngular().setX(xAng); //Set angular velocities
                    velCommand.getAngular().setY(yAng);
                    velCommand.getAngular().setZ(zAng);
                    pub.publish(velCommand); //Publish the twist message
                    Thread.sleep(100); //Do a delay of 100ms to let the message to be captured by the robot
                }

            }
        });
    }
}

public class MainActivity extends RosActivity {


    private Talker talker; // Define new twist publisher
    private JoystickView joystick; // Joystick control widget
    private VisualizationView visualizationView; //Map viewer widget
    private RosImageView<sensor_msgs.CompressedImage> image; //Camera widget
    private Button btnFwd; //Forward button
    private Button btnBwd; //Backward button
    private Button btnRight; //Right button
    private Button btnLeft; //Left button
    private MapPosePublisherLayer mapPosePublisherLayer; //Object to be used to capture the gestures from the map viewer and publish the corresponding goals to the robot

    public MainActivity() {
        // The RosActivity constructor configures the notification title and ticker
        // messages.
        super("Android Turtlebot", "Android Turtlebot");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // referring as others views
        joystick = (JoystickView) findViewById(R.id.joystickView);

        btnFwd = (Button)findViewById(R.id.buttonFWD);
        btnBwd = (Button)findViewById(R.id.buttonBWD);
        btnRight = (Button)findViewById(R.id.buttonRIGHT);
        btnLeft = (Button)findViewById(R.id.buttonLEFT);

        //Buttons touch listeners for setting linear/angular velocities:
        btnFwd.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {

                    talker.xVel = 0.3;
                    talker.zAng = 0;
                    talker.message_enable=true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    talker.xVel = 0;
                    talker.message_enable=false;

                }
                return true;
            }
        });


        btnBwd.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {

                    talker.xVel = -0.3;
                    talker.zAng = 0;
                    talker.message_enable=true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    talker.xVel = 0;
                    talker.message_enable=false;
                }
                return true;
            }
        });


        btnRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {

                    talker.xVel = 0;
                    talker.zAng = -1;
                    talker.message_enable=true;

                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    talker.zAng = 0;
                    talker.message_enable=false;
                }
                return true;
            }
        });


        btnLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {

                    talker.xVel = 0;
                    talker.zAng = 1;
                    talker.message_enable=true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    talker.zAng = 0;;
                    talker.message_enable=false;
                }
                return true;
            }
        });


        visualizationView = (VisualizationView) findViewById(R.id.visualization);
        visualizationView.getCamera().setFrame("map");
        image = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.image);
        image.setTopicName("camera/rgb/image_color/compressed"); //Subscribe camera widget to the corresponding topic
        image.setMessageType(sensor_msgs.CompressedImage._TYPE); //Treat the incoming data as compressed image
        image.setMessageToBitmapCallable(new BitmapFromCompressedImage());



        // Listener of events from joystick, it will return the angle in graus and power in percents
        // return to the direction of the movement
        joystick.setOnJoystickMoveListener(new OnJoystickMoveListener() {
            @Override
            public void onValueChanged(int angle, int power, int direction) {

                //Convert joystick values into actual robot velocities:

                if(angle==0 && power==0) {
                    talker.message_enable=false;
                }else {
                    talker.message_enable=true;
                    if (angle > 90 && angle < 180) {
                        //If it's inside the bottom-right quadrant:
                        talker.xVel = -(power / 100.0) / 4;
                        talker.zAng = (angle / 180.0 - 1) * 1.8;
                    } else if (angle < -90 && angle > -180) {
                        //If it's inside the bottom-left quadrant:
                        talker.xVel = -(power / 100.0) / 4;
                        talker.zAng = (angle / 180.0 + 1) * 1.8;
                    } else if (angle >= -90 && angle <= 90) {
                        //If it's inside the two top quadrants:
                        talker.xVel = (power / 100.0) / 4;
                        talker.zAng = -(angle / 90.0) / 1.2;
                    }
                }
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);

    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {

        talker = new Talker();

        visualizationView.addLayer(new CameraControlLayer(this, nodeMainExecutor
                .getScheduledExecutorService()));
        visualizationView.addLayer(new OccupancyGridLayer("map")); //Subscribe to the map topic
        visualizationView.addLayer(new PathLayer("move_base/TrajectoryPlannerROS/global_plan")); //Retrieve the paths planned by the robot
        visualizationView.addLayer(new LaserScanLayer("scan")); //Retrieve the Lidar readings
        visualizationView.addLayer(new PoseSubscriberLayer("move_base_simple/goal")); //Subscribe to the current position of the robot
        mapPosePublisherLayer = new MapPosePublisherLayer(this);
        visualizationView.addLayer(mapPosePublisherLayer); //Create a new goal pose publisher
        visualizationView.addLayer(new InitialPoseSubscriberLayer("initialpose")); //Create a new initial pose publisher
        visualizationView.addLayer(new RobotLayer("base_link")); //Retrieve robot status


        NodeConfiguration nodeConfiguration = null;
        try {
            nodeConfiguration = NodeConfiguration.newPublic("192.168.0.101", new URI("http://192.168.0.100:11311/")); //Set local and remote IPs
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        nodeMainExecutor.execute(talker, nodeConfiguration);

        nodeMainExecutor.execute(visualizationView, nodeConfiguration.setNodeName("android/map_view"));

        nodeMainExecutor.execute(image, nodeConfiguration.setNodeName("android/camera_view"));


    }

    public void setPoseClicked(View view) {
        setPose();
    }

    public void setGoalClicked(View view) {
        setGoal();
    }

    private void setPose() {
        mapPosePublisherLayer.setPoseMode();
    }

    private void setGoal() {
        mapPosePublisherLayer.setGoalMode();
    }

    public void dummyClick(View view) {

    }

}
