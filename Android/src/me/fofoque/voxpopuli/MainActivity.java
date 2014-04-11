package me.fofoque.voxpopuli;

import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.os.Bundle;
import android.widget.ToggleButton;

/**
 * This is the main activity of the HelloIOIO example application.
 * 
 * It displays a toggle button on the screen, which enables control of the
 * on-board LED. This example shows a very simple usage of the IOIO, by using
 * the {@link IOIOActivity} class. For a more advanced use case, see the
 * HelloIOIOPower example.
 */
public class MainActivity extends IOIOActivity {

	/**
	 * Called when the activity is first created. Here we normally initialize
	 * our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}

	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 */
	class Looper extends BaseIOIOLooper {
		private static final int PANSWITCH0 = 35;
		private static final int PANSWITCH1 = 39;
		private static final int TILTSWITCH0 = 43;
		private static final int TILTSWITCH1 = 46; // soon

		private static final int PANMOTOR0 = 12;
		private static final int PANMOTOR1 = 14;
		private static final int TILTMOTOR0 = 3;
		private static final int TILTMOTOR1 = 6;

		private static final int PWMFREQ = 2000;

		// these should be in a class...
		private DigitalInput panSwitch0, panSwitch1, tiltSwitch0, tiltSwitch1;
		private PwmOutput panMotor0, panMotor1, tiltMotor0, tiltMotor1;

		// state
		private int panDirection, tiltDirection;
		private float currentPanDutyCycle, currentTiltDutyCycle;

		/**
		 * Called every time a connection with IOIO has been established.
		 * Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException {
			panSwitch0 = ioio_.openDigitalInput(PANSWITCH0, DigitalInput.Spec.Mode.PULL_UP);
			panSwitch1 = ioio_.openDigitalInput(PANSWITCH1, DigitalInput.Spec.Mode.PULL_UP);
			tiltSwitch0 = ioio_.openDigitalInput(TILTSWITCH0, DigitalInput.Spec.Mode.PULL_UP);
			tiltSwitch1 = ioio_.openDigitalInput(TILTSWITCH1, DigitalInput.Spec.Mode.PULL_UP);

			panMotor0 = ioio_.openPwmOutput(PANMOTOR0, PWMFREQ);
			panMotor1 = ioio_.openPwmOutput(PANMOTOR1, PWMFREQ);
			tiltMotor0 = ioio_.openPwmOutput(TILTMOTOR0, PWMFREQ);
			tiltMotor1 = ioio_.openPwmOutput(TILTMOTOR1, PWMFREQ);

			panDirection = tiltDirection = 1;
			currentPanDutyCycle = currentTiltDutyCycle = 0.0f;
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		public void loop() throws ConnectionLostException {
			try {
				if(!panSwitch0.read()){
					panDirection = -1;
					currentPanDutyCycle = 0.0f;
				}
				if(!panSwitch1.read()){
					panDirection = 1;
					currentPanDutyCycle = 0.0f;
				}

				if(!tiltSwitch0.read()){
					tiltDirection = -1;
					currentTiltDutyCycle = 0.0f;
				}
				if(!tiltSwitch1.read()){
					tiltDirection = 1;
					currentTiltDutyCycle = 0.0f;
				}

				panMotor0.setDutyCycle((panDirection>0)?currentPanDutyCycle:0.0f);
				panMotor1.setDutyCycle((panDirection>0)?0.0f:currentPanDutyCycle);

				tiltMotor0.setDutyCycle((tiltDirection>0)?currentTiltDutyCycle:0.0f);
				tiltMotor1.setDutyCycle((tiltDirection>0)?0.0f:currentTiltDutyCycle);

				currentPanDutyCycle += (currentPanDutyCycle<0.5)?0.005f:0.0f;
				currentTiltDutyCycle += (currentTiltDutyCycle<0.5)?0.005f:0.0f;

				Thread.sleep(10);
			}
			catch (InterruptedException e) { }
		}
	}

	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
}