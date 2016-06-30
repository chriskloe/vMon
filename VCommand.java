package vMon;

/***
The VCommand class saves the name, the description and the value of an vctrld command
***/
public class VCommand {
	private String mName = new String();
	private String mDescription = new String();
	private String mValue = new String ();
	
	public VCommand (String theString)
	{
		String[] parts = theString.split(": ");
		setmName(parts[0]);
		setmDescription(parts[1]);
	}
	
	public boolean updateValue(String theValue)
	{
		if (theValue != getmValue())
		{
			setValue(theValue);
			return true;
		}
		return false;
	}
	
	private void setValue(String theValue) 
	{
		setmValue(theValue);
	}

	public String getmName() {
		return mName;
	}

	private void setmName(String mName) {
		this.mName = mName;
	}

	public String getmDescription() {
		return mDescription;
	}

	private void setmDescription(String mDescription) {
		this.mDescription = mDescription;
	}

	public String getmValue() {
		return mValue;
	}

	public void setmValue(String mValue) {
		String lines[] = mValue.split("\\r?\\n");
		this.mValue = lines[0].replaceAll("\\s+$", "");
	}
}
