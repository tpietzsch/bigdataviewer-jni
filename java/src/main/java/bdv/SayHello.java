package bdv;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;


public class SayHello
{
	public static void main( final String[] args )
	{
		show();
	}

	public static void show()
	{
		System.out.println("hello from show()");
		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				new JFrame( "SHOW" ).setVisible( true );
			}
		} );
		System.out.println("goodbye from show()");
	}
}
