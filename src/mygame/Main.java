package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.util.SkyFactory;

/**
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *          PLEASE NOTE THAT THIS FILE IS JUST HERE FOR jME3 SDK - 
 *                not for the actual game entry point
 * 
 * 
 * 
 * 
 * 
 *              The actual entry point is the core.clj.
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * @author CmdrDats
 */
public class Main extends SimpleApplication {

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        Node n = new Node();
        rootNode.attachChild(getAssetManager().loadModel("Models/thingy/thingy.j3o"));
        Spatial createSky = SkyFactory.createSky(getAssetManager(), "Textures/cloud.JPG", true);
        rootNode.attachChild(createSky);
        
    }

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
