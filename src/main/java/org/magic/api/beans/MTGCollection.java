package org.magic.api.beans;

import org.magic.api.interfaces.extra.MTGSerializable;

public class MTGCollection implements MTGSerializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String name;

	public MTGCollection() {

	}

	public MTGCollection(String name) {
		this.name = name;
	}

	public void setName(String string) {
		this.name = string;

	}

	@Override
	public String toString() {
		return getName();
	}

	public String getName() {
		return name;
	}


	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public String getStoreId() {
		return getName();
	}


	@Override
	public boolean equals(Object obj) {

		if(!(obj instanceof MTGCollection))
			return false;

		return ((MTGCollection)obj).getName().equalsIgnoreCase(getName());
	}
	

	

}
