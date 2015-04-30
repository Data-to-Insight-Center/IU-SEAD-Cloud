package org.seadva.registry.database.model.obj.vaRegistry;


import com.google.gson.annotations.Expose;
import org.seadva.registry.database.model.obj.vaRegistry.iface.IFixityPK;


/** 
 * Object mapping for hibernate-handled table: fixity.
 * @author autogenerated
 */


public class FixityPK implements  IFixityPK {

	/** Serial Version UID. */
	private static final long serialVersionUID = -559002632L;

	

	/** Field mapping. */
    @Expose
	private File entity;

	/** Field mapping. */
    @Expose
	private String type;

 


 
	/** Return the type of this class. Useful for when dealing with proxies.
	* @return Defining class.
	*/
	public Class<?> getClassType() {
		return FixityPK.class;
	}
 

    /**
     * Return the value associated with the column: entity.
	 * @return A File object (this.entity)
	 */
	public File getEntity() {
		return this.entity;
		
	}
	

  
    /**  
     * Set the value related to the column: entity.
	 * @param entity the entity value you wish to set
	 */
	public void setEntity(final File entity) {
		this.entity = entity;
	}

    /**
     * Return the value associated with the column: type.
	 * @return A String object (this.type)
	 */
	public String getType() {
		return this.type;
		
	}
	

  
    /**  
     * Set the value related to the column: type.
	 * @param type the type value you wish to set
	 */
	public void setType(final String type) {
		this.type = type;
	}


   /**
    * Deep copy.
	* @return cloned object
	* @throws CloneNotSupportedException on error
    */
    @Override
    public FixityPK clone() throws CloneNotSupportedException {
		
        final FixityPK copy = (FixityPK)super.clone();

		copy.setType(this.getType());
		return copy;
	}
	


	/** Provides toString implementation.
	 * @see Object#toString()
	 * @return String representation of this class.
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("type: " + this.getType());
		return sb.toString();		
	}


	/** Equals implementation. 
	 * @see Object#equals(Object)
	 * @param aThat Object to compare with
	 * @return true/false
	 */
	@Override
	public boolean equals(final Object aThat) {
		Object proxyThat = aThat;
		
		if ( this == aThat ) {
			 return true;
		}

		if (aThat == null)  {
			 return false;
		}
		
		final FixityPK that; 
		try {
			that = (FixityPK) proxyThat;
			if ( !(that.getClassType().equals(this.getClassType()))){
				return false;
			}
		} catch (org.hibernate.ObjectNotFoundException e) {
				return false;
		} catch (ClassCastException e) {
				return false;
		}
		
		
		boolean result = true;
		result = result && (((getEntity() == null) && (that.getEntity() == null)) || (getEntity() != null && getEntity().getId().equals(that.getEntity().getId())));	
		result = result && (((getType() == null) && (that.getType() == null)) || (getType() != null && getType().equals(that.getType())));
		return result;
	}
	
	/** Calculate the hashcode.
	 * @see Object#hashCode()
	 * @return a calculated number
	 */
	@Override
	public int hashCode() {
	int hash = 0;
		hash = hash + getEntity().hashCode();
		hash = hash + getType().hashCode();
	return hash;
	}
	

	
}