package com.example.hungrysnake;

import android.content.Context;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Locale;
import java.util.Random;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.ToneGenerator;


class SnakeEngine extends SurfaceView implements Runnable {

    private Thread thread = null;

    // To hold a reference to the Activity
    private Context context;

    // For tracking movement Heading
    public enum Heading {UP, RIGHT, DOWN, LEFT}
    // Start by heading to the right
    private Heading heading = Heading.RIGHT;

    // To hold the screen size in pixels
    private int screenX;
    private int screenY;

    // How long is the snake
    private int snakeLength;

    // Where is Bob hiding?
    private int bobX;
    private int bobY;

    // The size in pixels of a snake segment
    private int blockSize;

    // The size in segments of the playable area
    private final int NUM_BLOCKS_WIDE = 10;
    private int numBlocksHigh;

    // Control pausing between updates
    private long nextFrameTime;
    // Update the game 10 times per second
    private final long FPS = 1;
    // There are 1000 milliseconds in a second
    private final long MILLIS_PER_SECOND = 2000;
// We will draw the frame much more often

    // How many points does the player have
    private int score;

    // The location in the grid of all the segments
    private int[] snakeXs;
    private int[] snakeYs;

    // Everything we need for drawing
    // Is the game currently playing?
    private volatile boolean isPlaying;

    // A canvas for our paint
    private Canvas canvas;

    // Required to use canvas
    private SurfaceHolder surfaceHolder;

    // Some paint for our canvas
    private Paint paint;

    TextToSpeech tts;

    double prevDistanceToBob = Double.MAX_VALUE;

    ToneGenerator toneGenerator;


    public SnakeEngine(Context context, Point size) {
        super(context);
        this.context = context;
        screenX = size.x;
        screenY = size.y;

        // Work out how many pixels each block is
        blockSize = screenX / NUM_BLOCKS_WIDE;
        // How many blocks of the same size will fit into the height
        numBlocksHigh = screenY / blockSize;

        // Initialize the drawing objects
        surfaceHolder = getHolder();
        paint = new Paint();

        // If you score 200 you are rewarded with a crash achievement!
        snakeXs = new int[200];
        snakeYs = new int[200];

        // Start the game
        newGame();
    }

    public void run() {
        while (isPlaying) {
            // Update 10 times a second
            if(updateRequired()) {
                update();
                draw();
            }
        }
    }

    public void pause() {
        isPlaying = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            // Error
        }
    }

    public void resume() {
        isPlaying = true;
        thread = new Thread(this);
        thread.start();
    }


    public void newGame() {
        // Start with a single snake segment
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
        tts = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i!=TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.getDefault());
                }
            }
        });

        snakeLength = 1;
        snakeXs[0] = NUM_BLOCKS_WIDE / 2;
        snakeYs[0] = numBlocksHigh / 2;

        // Get Bob ready for dinner
        spawnBob();

        // Reset the score
        score = 0;

        // Setup nextFrameTime so an update is triggered
        nextFrameTime = System.currentTimeMillis();
        tts.speak("Rozpoczynanie gry", TextToSpeech.QUEUE_ADD, null);
    }

    public void spawnBob() {
        Random random = new Random();
        bobX = random.nextInt(NUM_BLOCKS_WIDE - 1) + 1;
        bobY = random.nextInt(numBlocksHigh - 1) + 1;
    }


    private void eatBob(){
        //  Got him!
        // Increase the size of the snake
        snakeLength++;
        //replace Bob
        // This reminds me of Edge of Tomorrow. Oneday Bob will be ready!
        spawnBob();
        //add to the score

        score = score + 1;
        String scoreTTS;
        if(score != 1) {
            scoreTTS = "Masz " + Integer.toString(score) + "punkty";
        } else {
            scoreTTS = "Masz " + Integer.toString(score) + "punkt";
        }
        tts.speak(scoreTTS, TextToSpeech.QUEUE_ADD, null);
    }

    private void moveSnake(){
        // Move the body
        for (int i = snakeLength; i > 0; i--) {
            // Start at the back and move it
            // to the position of the segment in front of it
            snakeXs[i] = snakeXs[i - 1];
            snakeYs[i] = snakeYs[i - 1];
            // Exclude the head because
            // the head has nothing in front of it
        }
        tts.speak("pip", TextToSpeech.QUEUE_ADD, null);

        double distanceToBob = Math.sqrt(Math.pow(bobX - snakeXs[0], 2) + Math.pow(bobY - snakeYs[0], 2));

        if(distanceToBob < prevDistanceToBob) {
            tts.speak("Ciepło", TextToSpeech.QUEUE_ADD, null);
        } else {
            tts.speak("Zimno", TextToSpeech.QUEUE_ADD, null);
        }
        prevDistanceToBob = distanceToBob;
        // Move the head in the appropriate heading
        switch (heading) {
            case UP:
                snakeYs[0]--;
                int distanceToTop = Math.abs(numBlocksHigh - (numBlocksHigh + snakeYs[0]));
                if(distanceToTop < 3) {
                    String distanceToTopTTS = "T " + Integer.toString(distanceToTop);
                    tts.speak(distanceToTopTTS, TextToSpeech.QUEUE_ADD, null);
                }

                break;
            case RIGHT:
                snakeXs[0]++;
                int distanceToRight = Math.abs(NUM_BLOCKS_WIDE - snakeXs[0]);
                if(distanceToRight < 3) {
                    String distanceToRightTTS = "R " + Integer.toString(distanceToRight);
                    tts.speak(distanceToRightTTS, TextToSpeech.QUEUE_ADD, null);
                }
                break;
            case DOWN:
                snakeYs[0]++;
                int distanceToDown = Math.abs((numBlocksHigh - snakeYs[0]));
                if(distanceToDown < 3) {
                    String distanceToDownTTS = "D " + Integer.toString(distanceToDown);
                    tts.speak(distanceToDownTTS, TextToSpeech.QUEUE_ADD, null);
                }
                break;
            case LEFT:
                snakeXs[0]--;
                int distanceToLeft = Math.abs(NUM_BLOCKS_WIDE - (NUM_BLOCKS_WIDE + snakeXs[0]));
                if(distanceToLeft < 3) {
                    String distanceToLeftTTS = "L " + Integer.toString(distanceToLeft);
                    tts.speak(distanceToLeftTTS, TextToSpeech.QUEUE_ADD, null);
                }
                break;
        }
    }

    private boolean detectDeath(){
        // Has the snake died?
        boolean dead = false;

        // Hit the screen edge
        if (snakeXs[0] == -1) dead = true;
        if (snakeXs[0] >= NUM_BLOCKS_WIDE) dead = true;
        if (snakeYs[0] == -1) dead = true;
        if (snakeYs[0] == numBlocksHigh) dead = true;
        if(dead == true) tts.speak("Uderzono w ścianę", TextToSpeech.QUEUE_ADD, null);

        // Eaten itself?
        for (int i = snakeLength - 1; i > 0; i--) {
            if ((i > 4) && (snakeXs[0] == snakeXs[i]) && (snakeYs[0] == snakeYs[i])) {
                dead = true;
                tts.speak("Zjadłeś swój ogon", TextToSpeech.QUEUE_ADD, null);
            }
        }
        return dead;
    }

    public void update() {
        // Did the head of the snake eat Bob?
        if (snakeXs[0] == bobX && snakeYs[0] == bobY) {
            eatBob();
        }
        moveSnake();
        if (detectDeath()) {
            //start again
            tts.speak("Rozpoczynanie nowej gry", TextToSpeech.QUEUE_FLUSH, null);
            newGame();
        }
    }

    public void draw() {
        // Get a lock on the canvas
        if (surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();
            // Fill the screen with Game Code School blue
            canvas.drawColor(Color.argb(255, 26, 128, 182));
            // Set the color of the paint to draw the snake white
            paint.setColor(Color.argb(255, 255, 0, 0));
            // Scale the HUD text
            paint.setTextSize(20);
            paint.setColor(Color.argb(255, 255, 255, 255));
            canvas.drawText("Score:" + score, 10, 70, paint);
            // Draw the snake one block at a time
            for (int i = 0; i < snakeLength; i++) {
                canvas.drawRect(snakeXs[i] * blockSize,
                        (snakeYs[i] * blockSize),
                        (snakeXs[i] * blockSize) + blockSize,
                        (snakeYs[i] * blockSize) + blockSize,
                        paint);
            }

            // Set the color of the paint to draw Bob red
            paint.setColor(Color.argb(255, 0, 255, 0));
            // Draw Bob
            canvas.drawRect(bobX * blockSize,
                    (bobY * blockSize),
                    (bobX * blockSize) + blockSize,
                    (bobY * blockSize) + blockSize,
                    paint);

            // Unlock the canvas and reveal the graphics for this frame
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }



    public boolean updateRequired() {

        // Are we due to update the frame
        if(nextFrameTime <= System.currentTimeMillis()){
            // Tenth of a second has passed
            // Setup when the next update will be triggered
            nextFrameTime =System.currentTimeMillis() + MILLIS_PER_SECOND / FPS;
            // Return true so that the update and draw
            // functions are executed
            return true;
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                if (motionEvent.getX() >= screenX/ 2) {
                    switch(heading){
                        case UP:
                            tts.speak("Skręcono w prawo", TextToSpeech.QUEUE_ADD, null);
                            heading = Heading.RIGHT;
                            break;
                        case RIGHT:
                            tts.speak("Skręcono w dół", TextToSpeech.QUEUE_ADD, null);
                            heading = Heading.DOWN;
                            break;
                        case DOWN:
                            tts.speak("Skręcono w lewo", TextToSpeech.QUEUE_ADD, null);
                            heading = Heading.LEFT;
                            break;
                        case LEFT:
                            tts.speak("Skręcono w górę", TextToSpeech.QUEUE_ADD, null);
                            heading = Heading.UP;
                            break;
                    }
                } else {
                    switch(heading){
                        case UP:
                            tts.speak("Skręcono w lewo", TextToSpeech.QUEUE_ADD, null);
                            heading = Heading.LEFT;
                            break;
                        case LEFT:
                            tts.speak("Skręcono w dół", TextToSpeech.QUEUE_ADD, null);
                            heading = Heading.DOWN;
                            break;
                        case DOWN:
                            tts.speak("Skręcono w prawo", TextToSpeech.QUEUE_ADD, null);
                            heading = Heading.RIGHT;
                            break;
                        case RIGHT:
                            tts.speak("Skręcono w górę", TextToSpeech.QUEUE_ADD, null);
                            heading = Heading.UP;
                            break;
                    }
                }
        }
        return true;
    }
}