package org.vishia.bridgeC;

public class ExampleTestFloat
{

	public final static void main(String[] args)
	{
		ExampleTestFloat main = new ExampleTestFloat();
		main.execute();
	}

	
	ExampleTestFloat()
	{
		
	}
	

	void execute()
	{
		showFloat(0.1f);
		showFloat(0.3f);
		showFloat(0.5f);
		showFloat(0.6f);
		showFloat(0.75f);
		showFloat(1.0f);
		showFloat(-1.0f);
		showFloat(2.0f);
		showFloat(10.0f);
		showFloat(13.88f);
		showFloat(1e38f);
		showFloat(1e-38f);
		stop();
	}
	
	void showFloat(float value)
	{
    int testi = Float.floatToIntBits(value);
    int mantisse = (testi & 0x7fffff ) | 0x800000;
    int exp = (testi >>23) & 0xff;
    System.out.printf("%7.7g = 0x%08x = exp=%02x mant=%06x \n", value, testi, exp, mantisse);
  	stop();
		
	}
	
	
	static void stop(){}
}
