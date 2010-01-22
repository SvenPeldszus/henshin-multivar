package org.eclipse.emf.henshin.statespace.impl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.henshin.interpreter.interfaces.InterpreterEngine;
import org.eclipse.emf.henshin.statespace.State;
import org.eclipse.emf.henshin.statespace.StateSpace;
import org.eclipse.emf.henshin.statespace.StateSpaceManager;
import org.eclipse.emf.henshin.statespace.TaintedStateSpaceException;
import org.eclipse.emf.henshin.statespace.Transition;
import org.eclipse.emf.henshin.statespace.util.StateSpaceSearch;
import org.eclipse.emf.henshin.statespace.util.StateSpaceSearch.Path;

/**
 * Default state space manager implementation.
 * 
 * @author Christian Krause
 * @generated NOT
 */
public class StateSpaceManagerImpl extends AbstractStateSpaceManagerWithIndex {
	
	// Default memory usage: 10%
	public static final double DEFAULT_MEMORY_USAGE = 0.1;

	// Percentage of models that are kept in memory:
	private double memoryUsage = DEFAULT_MEMORY_USAGE;
	
	// State model cache:
	private StateModelCache cache;
	
	/*
	 * Private constructor.
	 */
	private StateSpaceManagerImpl(StateSpace stateSpace) {
		super(stateSpace);
		this.cache = new StateModelCache();
	}
	
	/**
	 * Create a new state space manager instance.
	 * @param stateSpace State space.
	 * @param monitor Progress monitor.
	 * @return State space manager.
	 */
	public static StateSpaceManager load(StateSpace stateSpace, IProgressMonitor monitor) throws TaintedStateSpaceException {
		StateSpaceManagerImpl manager = new StateSpaceManagerImpl(stateSpace);
		manager.reload(monitor);
		return manager;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.emf.henshin.statespace.StateSpaceManager#getModel(org.eclipse.emf.henshin.statespace.State)
	 */
	public Resource getModel(State state) {
		
		// Model already set?
		if (state.getModel()!=null) {
			return state.getModel();
		}
		
		// Cached?
		Resource cached = cache.get(state);
		if (cached!=null) {
			return cached;
		}
		
		// Find a predecessor state that has a model:
		StateSpaceSearch search = new StateSpaceSearch() {
			@Override
			protected boolean shouldStop(State current, Path path) {
				return current.getModel()!=null || cache.get(current)!=null;
			}
		};
		boolean found = search.depthFirst(state, true);
		if (!found) {
			throw new RuntimeException("Unable to derive state model for state " + state.getName());
		}
		
		// Get the predecessor's model:
		Resource model = search.getState().getModel();
		if (model==null) model = cache.get(search.getState());
		
		// Derive the model for the current state:
		for (Transition transition : search.getPath()) {
						
			
		}
		
		// Decide whether the current model should be kept in memory:
		int states = getStateSpace().getStates().size();
		int stored = (int) (states * memoryUsage);			
		boolean storeCurrent = (stored>0) && (states % stored)==0;
		
		// Associated the model with the state (or not):
		state.setModel(storeCurrent ? model : null);
		
		// Always add it to the cache (is maintained automatically):
		cache.put(state, model);
		
		// Done.
		return model;
		
	}
	

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.emf.henshin.statespace.StateSpaceManager#explore(org.eclipse.emf.henshin.statespace.State)
	 */
	public List<Transition> exploreState(State state) {

		// ----- DUMMY IMPLEMENTATION FOR TESTING ------------------------ //
		
		State created = createState(new ResourceImpl(), 0);
		int[] location = state.getLocation();
		location[1] += 100;
		created.setLocation(location);
		state.setOpen(false);
		created.setOpen(true);
		Transition transition = createTransition(state, null, 23);
		transition.setTarget(created);
		if (true) return null;
		
		// --------------------------------------------------------------- //
		
		Resource model = getModel(state);
		if (model==null) {
			throw new RuntimeException("State without model");
		}
		
		// Find all matches:
		/*List<RuleMatch> matches = engine.findAllMatches(rule);
		for (int i=0; i<matches.size(); i++) {
			RuleMatch match = matches.get(i);
			
			// Create a copy of the model.
			Resource transformed = new ResourceImpl();
			transformed.getContents().addAll(EcoreUtil.copyAll(model.getContents()));
			
			
		}
		*/
		
		List<Transition> transitions = new ArrayList<Transition>();

		
		// Find all matches:
		
		
		// Transform it:
		
		
		// Check if a corresponding state exists already:
/*		int hash = hashCode(transformed);
		if (getState(transformed, hash)!=null) {
			// Add an outgoing transition if not existent:
			
		} else {
			State newState = createState(transformed, hash);
			createTransition(newState, null, 0);
			
			// Remember the new state:
			newStates.add(newState);
			
		}
	*/	
		// Mark the state as closed:
		if (state.isOpen()) {
			state.setOpen(false);
		}
		
		// Done.
		return transitions;
		
	}
	
	/**
	 * Set the memory usage for this state space manager.
	 * Must be between 0 and 1, where 0 means that no model
	 * are kept in memory (except a cache of constant size)
	 * and 1 means that all models are kept in memory. 
	 * @param memoryUsage Percentage for the memory usage.
	 */
	public void setMemoryUsage(double memoryUsage) {
		this.memoryUsage = Math.max(Math.min(memoryUsage,1),0);
	}

}
