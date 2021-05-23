package renderer;

import geometries.Intersectable.GeoPoint;
import primitives.Color;
import primitives.Material;
import primitives.Point3D;
import primitives.Ray;
import primitives.Vector;
import scene.Scene;
import static primitives.Util.*;

import java.util.List;

import elements.LightSource;

/**
 * The class inherits from the abstract class RayTracerBase
 * 
 * @author Adiel
 *
 */
public class RayTracerBasic extends RayTracerBase {
	private static final double INITIAL_K = 1.0;
	private static final int MAX_CALC_COLOR_LEVEL = 10;
	private static final double MIN_CALC_COLOR_K = 0.001;

	/**
	 * RayTracerBasic constructor who gets a scene and runs the constructor of the
	 * master class
	 * 
	 * @param scene
	 */
	public RayTracerBasic(Scene scene) {
		super(scene);

	}

	@Override
	public Color traceRay(Ray ray) {
		// List<GeoPoint> intersections = scene.geometries.findGeoIntersections(ray);
		// if (intersections != null) {
		// GeoPoint closestPoint = ray.getClosestGeoPoint(intersections);

		GeoPoint closestPoint = findClosestIntersection(ray);

		return closestPoint == null ? scene.background : calcColor(closestPoint, ray);

	}

	/**
	 * The function calculates the color of point with all of kinds of lights. We
	 * use Phong's model of lightTogather to got the precise color on the
	 * intersection point. We combine The Diffuse light and Specular light and
	 * emission light
	 * 
	 * @param intersection for the point and geometry
	 * @param ray
	 * @return color with the diffuse and speculative effects
	 */
	private Color calcLocalEffects(GeoPoint intersection, Ray ray,double k) {
		Vector v = ray.getDir();// Vector from camera to the body
		Vector n = intersection.geometry.getNormal(intersection.point);
		double nv = alignZero(n.dotProduct(v));
		if (nv == 0)
			return Color.BLACK;
		Material material = intersection.geometry.getMaterial();
		int nShininess = material.nShininess;
		double kd = material.kD;
		double ks = material.kS;

		Color color = Color.BLACK;
		for (LightSource lightSource : scene.lights) {
			Vector l = lightSource.getL(intersection.point);// Vector from the light source to the point
			double nl = alignZero(n.dotProduct(l));
			if (nl * nv > 0) {// sign(nl)==sign(nv)
				double ktr=transparency(lightSource, l, n, intersection);
				if(ktr*k>MIN_CALC_COLOR_K) {
				//if (unshaded(lightSource, l, n, intersection)) {// Shading test
					Color lightIntensity = lightSource.getIntensity(intersection.point).scale(ktr);// The intensity of the light
					color = color.add(calcDiffusive(kd, l, n, lightIntensity), // source at a certain point
							calcSpecular(ks, l, n, v, nShininess, lightIntensity));
				}
			}
		}
		return color;
	}

	/**
	 * Auxiliary function that calculates the specular component
	 * 
	 * @param ks             for the attenuation factor
	 * @param l              for the vector from light source to the point
	 * @param n              for the vector n
	 * @param v              for the vector from the camera to the point
	 * @param nShininess     for the exponent
	 * @param lightIntensity for the light intesity
	 * @return the specular color of the point
	 */
	private Color calcSpecular(double ks, Vector l, Vector n, Vector v, int nShininess, Color lightIntensity) {

		Vector r = n.scale(2 * (l.dotProduct(n))).subtract(l).normalize();
		Vector vMinus = v.scale(-1);
		double vr = alignZero(vMinus.dotProduct(r));

		if (vr > 0) {
			return lightIntensity.scale(ks * Math.pow(vr, nShininess));
		} else
			return lightIntensity.scale(0);
	}

	/**
	 * Auxiliary function that calculates the diffuse component
	 * 
	 * @param kd             for the attenuation factor
	 * @param l              for vector L
	 * @param n              for vector n
	 * @param lightIntensity the Light intensity
	 * @return the diffusive color of the point
	 */
	private Color calcDiffusive(double kd, Vector l, Vector n, Color lightIntensity) {
		return lightIntensity.scale(kd * Math.abs(l.dotProduct(n)));

	}

	/**
	 * The function checks for shading between a point and the light source
	 * 
	 * @param light
	 * @param l
	 * @param n
	 * @param geopoint geometry and point
	 * @return true if there is no shading, otherwise false
	 */
	/*
	 * private boolean unshaded(LightSource light, Vector l, Vector n, GeoPoint
	 * geopoint) { Vector lightDirection = l.scale(-1); // from point to light
	 * source Vector delta = n.scale(n.dotProduct(lightDirection) > 0 ? DELTA
	 * :-DELTA); Point3D point = geopoint.point.add(delta); Ray lightRay = new
	 * Ray(point, lightDirection);
	 * 
	 * List<GeoPoint> intersections =
	 * scene.geometries.findGeoIntersections(lightRay,
	 * light.getDistance(geopoint.point)); return intersections == null; }
	 */

	private boolean unshaded(LightSource light, Vector l, Vector n, GeoPoint geopoint) {
		Vector lightDirection = l.scale(-1); // from point to light source

		Ray lightRay = new Ray(geopoint.point, lightDirection, n);// refactored ray head move

		List<GeoPoint> intersections = scene.geometries.findGeoIntersections(lightRay);
		if (intersections == null)
			return true;
		double lightDistance = alignZero(light.getDistance(geopoint.point));
		for (GeoPoint gp : intersections) {
			if (lightDistance >= alignZero(gp.point.distance(geopoint.point)) && gp.geometry.getMaterial().kT == 0)
				return false;
		}
		return true;
	}

	private GeoPoint findClosestIntersection(Ray ray) {
		GeoPoint closesPoint;
		List<GeoPoint> intersectionsList = scene.geometries.findGeoIntersections(ray);
		if (intersectionsList != null) {
			closesPoint = ray.getClosestGeoPoint(intersectionsList);
			return closesPoint;
		}
		return null;

	}

	/**
	 * Calculate color using recursive function
	 * 
	 * @param gp  the point of intersection
	 * @param ray for the ray
	 * @return the color
	 */
	private Color calcColor(GeoPoint gp, Ray ray) {
		return calcColor(gp, ray, MAX_CALC_COLOR_LEVEL, INITIAL_K).add(scene.ambientLight.getIntensity());
	}

	/**
	 * The function calculates the effect of the light sources on the point for
	 * which the color is calculated according to the simple pong model
	 * 
	 * @param ray          for the point that strikes the point
	 * @param intersection for the point and geometry
	 * @return color of ambient light of the scene
	 */
	private Color calcColor(GeoPoint intersection, Ray ray, int level, double k) {
		Color color = intersection.geometry.getEmission();
		color = color.add(calcLocalEffects(intersection, ray,k));
		return 1 == level ? color : color.add(calcGlobalEffects(intersection, ray, level, k));
	}

	/**
	 * Calculate global effect of the light on the color
	 * 
	 * @param geopoint for the point
	 * @param ray      for the ray
	 * @param level    for depth of recursion
	 * @param k
	 * @return the color
	 */
	private Color calcGlobalEffects(GeoPoint geopoint, Ray ray, int level, double k) {
		Color color = Color.BLACK;
		Material material = geopoint.geometry.getMaterial();
		double kr = material.kR;
		double kkr = k * kr;
		Vector n = geopoint.geometry.getNormal(geopoint.point);

		if (kkr > MIN_CALC_COLOR_K) {
			Ray reflectedRay = constructReflectedRay(n, geopoint.point, ray);

			GeoPoint reflectedPoint = findClosestIntersection(reflectedRay);
			if (reflectedPoint != null) {// If there are points of intersection

				color = color.add(calcColor(reflectedPoint, reflectedRay, level - 1, kkr).scale(kr));
			} else {
				color = scene.background.scale(kr);
			}
		}
		double kt = material.kT;
		double kkt = k * kt;
		if (kkt > MIN_CALC_COLOR_K) {
			Ray refractedRay = constructRefractedRay(n, geopoint.point, ray);

			GeoPoint refractedPoint = findClosestIntersection(refractedRay);
			if (refractedPoint != null) {// If there are points of intersection

				color = color.add(calcColor(refractedPoint, refractedRay, level - 1, kkt).scale(kt));
			} else {
				color = scene.background.scale(kt);

			}
		}
		return color;
	}

	private Ray constructReflectedRay(Vector n, Point3D point, Ray ray) {
		Vector v = ray.getDir();
		Vector r = v.add(n.scale(-2 * alignZero(v.dotProduct(n))));
		Ray lightRay = new Ray(point, r, n);
		// Vector delta = n.scale(alignZero(n.dotProduct(r)) > 0 ? DELTA : -DELTA);
		// Point3D pointDelta = point.add(delta);

		return lightRay;
	}

	private Ray constructRefractedRay(Vector n, Point3D point, Ray ray) {
		Vector v = ray.getDir();

		// Vector delta = n.scale(alignZero(n.dotProduct(v)) > 0 ? DELTA : -DELTA);
		// Point3D pointDelta = point.add(delta);

		// return new Ray(pointDelta, ray.getDir());
		Ray lightRay = new Ray(point, v, n);
		return lightRay;
	}

	private double transparency(LightSource ls, Vector l, Vector n, GeoPoint geopoint) {
		Vector lightDirection = l.scale(-1); // from point to light source

		Ray lightRay = new Ray(geopoint.point, lightDirection, n);// refactored ray head move

		double lightDistance = alignZero(ls.getDistance(geopoint.point));
		var intersections = scene.geometries.findGeoIntersections(lightRay);
		if (intersections == null)
			return 1.0;
		double ktr = 1.0;
		for (GeoPoint gp : intersections) {
			if (lightDistance >= alignZero(gp.point.distance(geopoint.point))) {
				ktr *= gp.geometry.getMaterial().kT;
				if (ktr < MIN_CALC_COLOR_K)
					return 0.0;
			}
		}
		return ktr;
	}


}
