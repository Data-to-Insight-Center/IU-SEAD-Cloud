package org.seadva.registry.database.model.obj.vaRegistry;


import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.seadva.registry.database.model.obj.vaRegistry.Event;
import org.seadva.registry.database.model.obj.vaRegistry.Transition;
import org.seadva.registry.database.model.obj.vaRegistry.iface.IEventType;


/** 
 * Object mapping for hibernate-handled table: event_type.
 * @author autogenerated
 */

public class EventType implements IEventType {

	/** Serial Version UID. */
	private static final long serialVersionUID = -559002636L;

	/** Use a WeakHashMap so entries will be garbage collected once all entities 
		referring to a saved hash are garbage collected themselves. */
	private static final Map<Serializable, String> SAVED_HASHES =
		Collections.synchronizedMap(new WeakHashMap<Serializable, String>());
	
	/** hashCode temporary storage. */
	private volatile String hashCode;
	

	/** Field mapping. */
	private Set<Event> events = new HashSet<Event>();

	/** Field mapping. */
	private String eventDescription;
	/** Field mapping. */
	private String eventName;
	/** Field mapping. */
	private String id;
	/** Field mapping. */
	private Set<Transition> transitions = new HashSet<Transition>();

	/**
	 * Default constructor, mainly for hibernate use.
	 */
	public EventType() {
		// Default constructor
	} 

	/** Constructor taking a given ID.
	 * @param id to set
	 */
	public EventType(String id) {
		this.id = id;
	}
	
	/** Constructor taking a given ID.
	 * @param eventDescription String object;
	 * @param eventName String object;
	 * @param id String object;
	 */
	public EventType(String eventDescription, String eventName, String id) {

		this.eventDescription = eventDescription;
		this.eventName = eventName;
		this.id = id;
	}
	
 


 
	/** Return the type of this class. Useful for when dealing with proxies.
	* @return Defining class.
	*/
	public Class<?> getClassType() {
		return EventType.class;
	}
 

    /**
     * Return the value associated with the column: event.
	 * @return A Set&lt;Event&gt; object (this.event)
	 */

	public Set<Event> getEvents() {
		return this.events;
		
	}
	
	/**
	 * Adds a bi-directional link of type Event to the events set.
	 * @param event item to add
	 */
	public void addEvent(Event event) {
		event.setEventType(this);
		this.events.add(event);
	}

  
    /**  
     * Set the value related to the column: event.
	 * @param event the event value you wish to set
	 */
	public void setEvents(final Set<Event> event) {
		this.events = event;
	}

    /**
     * Return the value associated with the column: eventDescription.
	 * @return A String object (this.eventDescription)
	 */

	public String getEventDescription() {
		return this.eventDescription;
		
	}
	

  
    /**  
     * Set the value related to the column: eventDescription.
	 * @param eventDescription the eventDescription value you wish to set
	 */
	public void setEventDescription(final String eventDescription) {
		this.eventDescription = eventDescription;
	}

    /**
     * Return the value associated with the column: eventName.
	 * @return A String object (this.eventName)
	 */

	public String getEventName() {
		return this.eventName;
		
	}
	

  
    /**  
     * Set the value related to the column: eventName.
	 * @param eventName the eventName value you wish to set
	 */
	public void setEventName(final String eventName) {
		this.eventName = eventName;
	}

    /**
     * Return the value associated with the column: id.
	 * @return A String object (this.id)
	 */

	public String getId() {
		return this.id;
		
	}
	

  
    /**  
     * Set the value related to the column: id.
	 * @param id the id value you wish to set
	 */
	public void setId(final String id) {
		// If we've just been persisted and hashCode has been
		// returned then make sure other entities with this
		// ID return the already returned hash code
		if ( (this.id == null ) &&
				(id != null) &&
				(this.hashCode != null) ) {
		SAVED_HASHES.put( id, this.hashCode );
		}
		this.id = id;
	}

    /**
     * Return the value associated with the column: transition.
	 * @return A Set&lt;Transition&gt; object (this.transition)
	 */

	public Set<Transition> getTransitions() {
		return this.transitions;
		
	}
	
	/**
	 * Adds a bi-directional link of type Transition to the transitions set.
	 * @param transition item to add
	 */
	public void addTransition(Transition transition) {
		transition.getId().setEventType(this);
		this.transitions.add(transition);
	}

  
    /**  
     * Set the value related to the column: transition.
	 * @param transition the transition value you wish to set
	 */
	public void setTransitions(final Set<Transition> transition) {
		this.transitions = transition;
	}


   /**
    * Deep copy.
	* @return cloned object
	* @throws CloneNotSupportedException on error
    */
    @Override
    public EventType clone() throws CloneNotSupportedException {
		
        final EventType copy = (EventType)super.clone();

		if (this.getEvents() != null) {
			copy.getEvents().addAll(this.getEvents());
		}
		copy.setEventDescription(this.getEventDescription());
		copy.setEventName(this.getEventName());
		copy.setId(this.getId());
		if (this.getTransitions() != null) {
			copy.getTransitions().addAll(this.getTransitions());
		}
		return copy;
	}
	


	/** Provides toString implementation.
	 * @see Object#toString()
	 * @return String representation of this class.
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("eventDescription: " + this.getEventDescription() + ", ");
		sb.append("eventName: " + this.getEventName() + ", ");
		sb.append("id: " + this.getId() + ", ");
		return sb.toString();		
	}


}