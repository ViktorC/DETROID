package main.java.net.viktorc.detroid.framework.tuning;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A timer class for only one timed action at a time. It is designed for time control in turn based applications. It 
 * uses one single thread without having to restart it until a delayed task actually gets executed. Its tick based timing
 * mechanism allows for time measurements even in concurrent environments in which the number of running threads exceed 
 * the number of available processors. Its accuracy is dependent on the resolution setting and the priorities of the 
 * concurrently running threads.
 * 
 * Whenever a new delayed control task needs to be submitted, the timer can be paused with the {@link #pause() pause} 
 * method, the delay, the resolution, and the action to be performed can be reset using the  {@link #setDelay(long) setDelay}, 
 * {@link #setResolution(long) setResolution}, and {@link #setCallBack(Runnable) setCallBack} methods, and finally the timer 
 * can be prompted to continue with the newly set delay, resolution and/or task with the {@link #goOn() goOn} method. If the 
 * timer once went off, it has to be restarted by setting the timing parameters and calling the {@link #start() start} method 
 * which restarts the thread from the single thread fixed thread pool.
 * 
 * @author Viktor
 *
 */
public class MatchTimer implements AutoCloseable {

	// A single thread fixed thread pool in case the timer needs to be restarted.
	private ExecutorService pool;
	private Runnable callBack;
	private AtomicLong ticks;
	private volatile long delay;
	private volatile long resolution;
	private volatile boolean cancel;
	private volatile boolean pause;
	private volatile boolean isAlive;
	
	/**
	 * Creates an instance according to the specified parameters.
	 * 
	 * @param callBack The delayed action to be performed.
	 * @param delay The delay in milliseconds. It is the minimum amount of time the task
	 * will be delayed by, as it might be delayed longer depending on the resolution.
	 * @param resolution The length in milliseconds of the periods between which the timer 
	 * checks the execution conditions such as whether the timer has been cancelled or paused, 
	 * or whether the delayed task is due.
	 */
	public MatchTimer(Runnable callBack, long delay, long resolution) {
		pool = Executors.newSingleThreadExecutor();
		this.delay = delay;
		this.resolution = resolution;
		this.callBack = callBack;
		ticks = new AtomicLong();
		setDelay(delay);
	}
	/**
	 * Returns whether the timer has been cancelled.
	 * 
	 * @return
	 */
	public boolean isCancelled() {
		return cancel;
	}
	/**
	 * Returns whether the timer is on pause.
	 * 
	 * @return
	 */
	public boolean isPuased() {
		return pause;
	}
	/**
	 * Returns whether the timer is still alive which means that it is either still waiting or
	 * currently executing the delayed task.
	 * 
	 * @return
	 */
	public boolean isAlive() {
		return isAlive;
	}
	/**
	 * Returns the resolution of the timer in milliseconds.
	 * 
	 * @return
	 */
	public long getResolution() {
		return resolution;
	}
	/**
	 * Returns how much time is left until the delayed task is due in milliseconds.
	 * 
	 * @return
	 */
	public long getDelay() {
		return (long) (ticks.get()*resolution);
	}
	/**
	 * Sets the resolution of the timer.
	 * 
	 * @param resolution The resolution in milliseconds.
	 */
	public void setResolution(long resolution) {
		this.resolution = resolution;
		setDelay(delay);
	}
	/**
	 * Sets by how much the delayed task should be delayed.
	 * 
	 * @param delay The delay in milliseconds.
	 */
	public void setDelay(long delay) {
		this.delay = delay;
		ticks.set((long) Math.ceil(((double) delay)/resolution));
	}
	/**
	 * Sets the delayed task.
	 * 
	 * @param callBack
	 */
	public void setCallBack(Runnable callBack) {
		this.callBack = callBack;
	}
	/**
	 * Pauses the timer causing it to wait for a signal either to go on or cancel.
	 */
	public void pause() {
		pause = true;
	}
	/**
	 * Continues a paused timer.
	 */
	public void goOn() {
		pause = false;
		synchronized (this) {
			notify();
		}
	}
	/**
	 * Cancels the timer which results in the timer thread stopping.
	 */
	public void cancel() {
		cancel = true;
		if (pause) {
			synchronized (this) {
				notify();
			}
		}
	}
	/**
	 * Starts the timer with the set parameters on a separate thread from a single thread fixed thread pool.
	 * From the moment this method is invoked to the moment the execution of the delayed task finishes, the
	 * timer is considered alive.
	 */
	public void start() {
		// If the thread is still alive when this method is called, wait until it finishes.
		synchronized (this) {
			while (isAlive) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		isAlive = true;
		cancel = false;
		pause = false;
		pool.submit(() -> {
			while (ticks.get() > 0) {
				try {
					Thread.sleep(resolution);
					synchronized (MatchTimer.this) {
						while (pause && !cancel)
							MatchTimer.this.wait();
					}
					if (cancel) {
						isAlive = false;
						synchronized (MatchTimer.this) {
							MatchTimer.this.notify();
						}
						return;
					}
					ticks.decrementAndGet();
				} catch (InterruptedException e) {
					isAlive = false;
					synchronized (MatchTimer.this) {
						MatchTimer.this.notify();
					}
					return;
				}
			}
			callBack.run();
			isAlive = false;
			synchronized (MatchTimer.this) {
				MatchTimer.this.notify();
			}
		});
	}
	@Override
	public void close() {
		cancel();
		pool.shutdown();
	}
	
}