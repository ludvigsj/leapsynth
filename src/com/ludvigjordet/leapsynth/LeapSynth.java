package com.ludvigjordet.leapsynth;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.swing.ExponentialRangeModel;
import com.jsyn.swing.PortModelFactory;
import com.jsyn.unitgen.FilterLowPass;
import com.jsyn.unitgen.LineOut;
import com.jsyn.unitgen.LinearRamp;
import com.jsyn.unitgen.SineOscillator;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.InteractionBox;

public class LeapSynth {
	
	private JFrame window;
	private Canvas canvas;
	private BufferStrategy strategy;
	private Controller controller;
	private boolean running = true;
	private InteractionBox ibox;
	private List<Point> fingerPositions;
	private Synthesizer synth;
	private SineOscillator osc;
	private FilterLowPass filter;
	private LineOut lineOut;
	private LinearRamp lag;
	private LinearRamp lag2;
	private ExponentialRangeModel filterModel;
	private ExponentialRangeModel toneModel;
	private float rz;
	private float ly;
	
	public LeapSynth(){
		init();
		while(running ) {
			tick();
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
		}
	}

	private void init() {
		window = new JFrame();
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window.setResizable(false);
		
		canvas = new Canvas();
		canvas.setSize(800, 600);
		
		window.add(canvas);
		window.pack();
		window.setVisible(true);
		
		canvas.createBufferStrategy(2);
		strategy = canvas.getBufferStrategy();
		
		controller = new Controller();
		
		fingerPositions = new ArrayList<>();
		
		synth = JSyn.createSynthesizer();
		synth.add(osc = new SineOscillator());
		synth.add(filter = new FilterLowPass());
		synth.add(lag = new LinearRamp() );
		synth.add(lag2 = new LinearRamp() );
		lag.output.connect(filter.frequency);
		
		filterModel = PortModelFactory.createExponentialModel( lag.input );
		toneModel = PortModelFactory.createExponentialModel(lag2.input);
		
		lag.time.set(0.2);
		lag2.time.set(0.2);
		lag2.output.connect(osc.frequency);
		osc.output.connect(filter.input);
		osc.amplitude.set(0.5);
		osc.frequency.set(440.0);
		filter.frequency.set(220.0);
		synth.add(lineOut = new LineOut());
		filter.output.connect(0, lineOut.input, 0);
		filter.output.connect(0, lineOut.input, 1);
		
		synth.start();
		lineOut.start();
		
		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e){
				synth.stop();
			}
		});
	}
	
	private void tick() {
		update();
		Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
		draw(g);
		g.dispose();
		strategy.show();
	}

	private void draw(Graphics2D g) {
		g.setColor(Color.black);
		g.fillRect(0, 0, 800, 600);
		g.setColor(Color.gray);
		for(int i = 1; i < 12; i++){
			g.drawLine(0, (int)(i*600.0/12.0), 800, (int)(i*600.0/12.0));
		}
		g.setColor(Color.yellow);
		g.setStroke(new BasicStroke(4+ly*16));
		g.drawLine(0, (int)(rz*600), 800, (int)(rz*600));
	}
	

	private void update() {
		fingerPositions.clear();
		ibox = controller.frame().interactionBox();
		rz = ibox.normalizePoint(controller.frame().hands().rightmost().palmPosition()).getZ();
		ly = ibox.normalizePoint(controller.frame().hands().leftmost().palmPosition()).getY();
		lag.input.set(ly*800);
		lag2.input.set(Math.pow(2,rz)*440);
	}
}
