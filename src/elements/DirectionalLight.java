package elements;

import primitives.Color;
import primitives.Point3D;
import primitives.Vector;

public class DirectionalLight extends Light implements LightSource {
	
	
	private Vector direction;

	/**
	 * DirectionalLight constructor initializes directional light with it's intensity and direction
	 *  
	 * @param intensity for the light
	 * @param direction for direction vector
	 */
	public DirectionalLight(Color intensity, Vector direction) {
		super(intensity);
		this.direction = direction.normalized();
	}

	@Override
	public Color getIntensity(Point3D p) {
		
		return super.getIntensity();
	}

	@Override
	public Vector getL(Point3D p) {
		
		return direction;
	}
	

}