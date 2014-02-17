package com.project.main;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.opengl.GLUtils;

import com.lib.math.GeometryUtils;
import com.lib.math.Intersector;
import com.lib.utils.FloatArray;
import com.lib.utils.ShortArray;
import com.project.data.MapaBits;

public abstract class OpenGLRenderer implements Renderer
{	
	// Par�metros de la C�mara
	private float xLeft, xRight, yTop, yBot, xCentro, yCentro;
	
	// Copia Seguridad de la C�mara
	private boolean camaraGuardada;
	private float lastXLeft, lastXRight, lastYTop, lastYBot, lastXCentro, lastYCentro;
	
	// Par�metros del Puerto de Vista
	private int height, width;
	
	//Par�metros de la Escena
	protected static final int SIZELINE = 3;
	protected static final int POINTWIDTH = 7;
	
	protected static final float MAX_DISTANCE_PIXELS = 10;
	
	// Par�metros de Texturas
	protected static final int numeroTexturas = 4;
	protected int[] nombreTexturas;
	
	// Marco
	private float marcoA, marcoB, marcoC;
	private FloatBuffer recMarcoA, recMarcoB;
	
	// Contexto
	protected Context context;
	
	/* Constructoras */
	
	public OpenGLRenderer(Context context)
	{
		this.context = context;
		this.nombreTexturas = new int[numeroTexturas];
		
		// Marcos
		actualizarMarcos();
		
		// Se inicializan los par�metros de la c�mara en el 
		// m�todo onSurfaceChanged llamado autom�ticamente
		// despu�s de la constructora.
	}
	
	/* M�todos abstractos a implementar */
	
	public abstract void onTouchDown(float x, float y, float width, float height, int pos);
	public abstract void onTouchMove(float x, float y, float width, float height, int pos);
	public abstract void onTouchUp(float x, float y, float width, float height, int pos);
	public abstract void onMultiTouchEvent();
	public abstract void reiniciar();
	
	/* M�todos de la interfaz Renderer */
	
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{		
		// Sombreado Suave
		gl.glShadeModel(GL10.GL_SMOOTH);
		
		// Color de Fondo Blanco
		gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		
		// Limpiar Buffer de Profundidad
		gl.glClearDepthf(1.0f);

		// Activar Test de Profundidad
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		
		// Activar Back-Face Culling
		gl.glEnable(GL10.GL_CULL_FACE);
		gl.glCullFace(GL10.GL_BACK);
		
		// Activar Transparencia
		gl.glEnable(GL10.GL_BLEND); 
	    gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
		
		// Perspectiva Ortogonal
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
		GLU.gluOrtho2D(gl, xLeft, xRight, yBot, yTop);
		
		// Reiniciar Matriz de ModeladoVista
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{		
		// Cambio de Puerto de Vista
		this.width = width;
		this.height = height;
		gl.glViewport(0, 0, width, height);
		
		// Perspectiva Ortogonal proporcional al Puerto de Vista
		this.xRight = width;
		this.xLeft = 0.0f;
		this.yTop = height;
		this.yBot = 0.0f;
		this.xCentro = (xRight + xLeft)/2.0f;
		this.yCentro = (yTop + yBot)/2.0f;
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		GLU.gluOrtho2D(gl, xLeft, xRight, yBot, yTop);
		
		// Copia de Seguridad de la C�mara
		this.camaraGuardada = false;
		
		// Marco
		actualizarMarcos();
		
		// Reiniciar la Matriz de ModeladoVista
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
	}
	
	@Override
	public void onDrawFrame(GL10 gl)
	{	
		// Persepectiva Ortogonal para m�todos de modificaci�n de la c�mara
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		GLU.gluOrtho2D(gl, xLeft, xRight, yBot, yTop);
		
		// Limpiar Buffer de Color y de Profundidad
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		
		// Activar Matriz de ModeladoVista
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
	}
	
	/* M�todos de Modificaci�n de C�mara */
	
	public void zoom(float factor)
	{	
		float newAncho = (xRight-xLeft)*factor;
		float newAlto = (yTop-yBot)*factor;
		
		this.xRight = xCentro + newAncho/2.0f;
		this.xLeft = xCentro - newAncho/2.0f;
		this.yTop = yCentro + newAlto/2.0f;
		this.yBot = yCentro - newAlto/2.0f;
		
		actualizarMarcos();
	}
	
	public void drag(float dWorldX, float dWorldY)
	{			
		this.xLeft += dWorldX;
		this.xRight += dWorldX;
		this.yBot += dWorldY;
		this.yTop += dWorldY;
		
		this.xCentro = (xRight + xLeft)/2.0f;
        this.yCentro = (yTop + yBot)/2.0f;
        
        actualizarMarcos();
	}
	
	public void drag(float pixelX, float pixelY, float lastPixelX, float lastPixelY, float screenWidth, float screenHeight)
	{
		float worldX = convertToWorldXCoordinate(pixelX, screenWidth);
		float worldY = convertToWorldYCoordinate(pixelY, screenHeight);
		
		float lastWorldX = convertToWorldXCoordinate(lastPixelX, screenWidth);
		float lastWorldY = convertToWorldYCoordinate(lastPixelY, screenHeight);
		
		float dWorldX = lastWorldX - worldX;
		float dWorldY = lastWorldY - worldY;
		
		drag(dWorldX, dWorldY);
	}
	
	public void restore()
	{
        this.xRight = width; 
        this.xLeft = 0.0f;
        this.yTop = height;
        this.yBot = 0.0f;
        
        this.xCentro = (xRight + xLeft)/2.0f;
        this.yCentro = (yTop + yBot)/2.0f;
        
        actualizarMarcos();
	}
	
	/* Copia de Seguridad de la C�mara */
	
	public void salvarCamara()
	{
		lastXLeft = xLeft;
		lastXRight = xRight;
		lastYTop = yTop;
		lastYBot = yBot;
		lastXCentro = xCentro;
		lastYCentro = yCentro;
		
		camaraGuardada = true;
	}
	
	public void recuperarCamara()
	{
		if(camaraGuardada)
		{
			xLeft = lastXLeft;
			xRight = lastXRight;
			yTop = lastYTop;
			yBot = lastYBot;
			xCentro = lastXCentro;
			yCentro = lastYCentro;
		}
	}
	
	/* Captura de Pantalla */
	
	private MapaBits capturaPantalla(GL10 gl, int leftX, int leftY, int height, int width)
	{
	    int screenshotSize = width * height;
	    ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
	    bb.order(ByteOrder.nativeOrder());
	    
	    gl.glReadPixels(leftX, leftY, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
	    
	    int pixelsBuffer[] = new int[screenshotSize];
	    bb.asIntBuffer().get(pixelsBuffer);
	    bb = null;

	    for (int i = 0; i < screenshotSize; ++i)
	    {
	        pixelsBuffer[i] = ((pixelsBuffer[i] & 0xff00ff00)) | ((pixelsBuffer[i] & 0x000000ff) << 16) | ((pixelsBuffer[i] & 0x00ff0000) >> 16);
	    }
	    
	    MapaBits textura = new MapaBits();
	    textura.setBitmap(pixelsBuffer, width, height);
	    return textura;
	}
	
	protected MapaBits capturaPantalla(GL10 gl, int height, int width)
	{
		return capturaPantalla(gl, 0, 0, width, height);
	}
	
	/*	
	____________________________________
	|			|___________|			| B
	|			|			|			|
	|			|			|			| A
	|			|			|			|
	|			|___________|			|
	|___________|___________|___________| B
		C
	 */
	
	protected MapaBits capturaPantallaPolariod(GL10 gl, int height, int width)
	{
		float marcoA = 0.8f * height;
		float marcoB = 0.1f * height;
		float marcoC = (width - marcoA)/2;
		
		return capturaPantalla(gl, (int) marcoC, (int) marcoB, (int) marcoA, (int) marcoA);
	}
	
	/* Conversi�n de Coordenadas */
	
	protected float convertToWorldXCoordinate(float pixelX, float screenWidth)
	{
		return xLeft + (xRight-xLeft)*pixelX/screenWidth;
	}
	
	protected float convertToWorldYCoordinate(float pixelY, float screenHeight)
	{
		return yBot + (yTop-yBot)*(screenHeight-pixelY)/screenHeight;
	}
	
	protected float convertToPixelXCoordinate(float worldX, float screenWidth)
	{
		return (worldX - xLeft)*screenWidth/(xRight-xLeft);
	}
	
	protected float convertToPixelYCoordinate(float worldY, float screenHeight)
	{
		return screenHeight - (worldY - yBot)*screenHeight/(yTop-yBot);
	}
	
	protected boolean inPixelInCanvas(float worldX, float worldY)
	{
		return worldX >= xLeft && worldX <= xRight && worldY >= yBot && worldY <= yTop;
	}
	
	/* B�squeda de Pixeles */
	
	protected short buscarPixel(FloatArray vertices, float pixelX, float pixelY, float screenWidth, float screenHeight)
	{
		float worldX = convertToWorldXCoordinate(pixelX, screenWidth);
		float worldY = convertToWorldYCoordinate(pixelY, screenHeight);
		
		if(!inPixelInCanvas(worldX, worldY)) return -1;
		
		int minpos = -1;
		
		int j = 0;
		while(j < vertices.size)
		{
			float px = vertices.get(j);
			float py = vertices.get(j+1);	
			
			float lastpx = convertToPixelXCoordinate(px, screenWidth);
			float lastpy = convertToPixelYCoordinate(py, screenHeight);
						
			float distancia = Math.abs(Intersector.distancePoints(pixelX, pixelY, lastpx, lastpy));
			if(distancia < MAX_DISTANCE_PIXELS)
			{
				minpos = j/2;
				return (short)minpos;
			}
			
			j = j+2;
		}
		
		return (short)minpos;
	}	
	
	protected short buscarPixel(ShortArray contorno, FloatArray vertices, float pixelX, float pixelY, float screenWidth, float screenHeight)
	{
		float worldX = convertToWorldXCoordinate(pixelX, screenWidth);
		float worldY = convertToWorldYCoordinate(pixelY, screenHeight);
				
		if(!GeometryUtils.isPointInsideMesh(contorno, vertices, worldX, worldY)) return -1;
		
		float mindistancia = Float.MAX_VALUE;
		int minpos = -1;
		
		int j = 0;
		while(j < vertices.size)
		{
			float px = vertices.get(j);
			float py = vertices.get(j+1);	
			
			float lastpx = convertToPixelXCoordinate(px, screenWidth);
			float lastpy = convertToPixelYCoordinate(py, screenHeight);
						
			float distancia = Math.abs(Intersector.distancePoints(pixelX, pixelY, lastpx, lastpy));
			if(distancia < mindistancia)
			{
				minpos = j/2;
				mindistancia = distancia;
			}
			
			j = j+2;
		}
		
		return (short)minpos;
	}	
	
	/* M�todos de Construcci�n de Buffer de Pintura */
	
	// Construcci�n de un buffer de pintura para puntos a partir de una lista de vertices
	// Uso para GL_POINTS o GL_LINE_LOOP
	protected FloatBuffer construirBufferListaPuntos(float[] vertices)
	{			
		ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		FloatBuffer buffer = byteBuf.asFloatBuffer();
		buffer.put(vertices);
		buffer.position(0);
		
		return buffer;
	}
	
	// Construcci�n de un buffer de pintura para puntos a partir de una lista de vertices
	// Uso para GL_POINTS o GL_LINE_LOOP
	protected FloatBuffer construirBufferListaPuntos(FloatArray vertices)
	{			
		float[] arrayVertices = new float[vertices.size];
		System.arraycopy(vertices.items, 0, arrayVertices, 0, vertices.size);
		
		return construirBufferListaPuntos(arrayVertices);
	}
	
	// Construcci�n de un buffer de pintura para puntos a partir de una lista de indice de vertices
	protected FloatBuffer construirBufferListaIndicePuntos(ShortArray indices, FloatArray vertices)
	{
		float[] arrayVertices = new float[2*indices.size];
		
		int i = 0;
		while(i < indices.size)
		{
			int pos = indices.get(i);
			arrayVertices[2*pos] = vertices.get(2*pos);
			arrayVertices[2*pos+1] = vertices.get(2*pos+1);
			
			i++;
		}
		
		return construirBufferListaPuntos(arrayVertices);
	}
	
	// Construcci�n de un buffer de pintura para lineas a partir de una lista de triangulos. 
	// Uso para GL_LINES
	protected FloatBuffer construirBufferListaLineas(ShortArray triangulos, FloatArray vertices)
	{
		int arrayLong = 2*(triangulos.size-1);
		float[] arrayVertices = new float[2*arrayLong];
		
		int j = 0;
		int i = 0;
		while(j < triangulos.size)
		{
			short a = triangulos.get(j);
			short b = triangulos.get(j+1);
			
			arrayVertices[i] = vertices.get(2*a);
			arrayVertices[i+1] = vertices.get(2*a+1);
			
			arrayVertices[i+2] = vertices.get(2*b);
			arrayVertices[i+3] = vertices.get(2*b+1);
			
			j = j+1;
			i = i+4;
		}	
		
		return construirBufferListaPuntos(arrayVertices);
	}
	
	// Construcci�n de un buffer de pintura para lineas a partir de una lista de triangulos.
	// Uso para GL_LINES
	protected FloatBuffer construirBufferListaTriangulos(ShortArray triangulos, FloatArray vertices)
	{
		int arrayLong = 2*triangulos.size;
		float[] arrayVertices = new float[2*arrayLong];
		
		int j = 0;
		int i = 0;
		while(j < triangulos.size)
		{
			short a = triangulos.get(j);
			short b = triangulos.get(j+1);
			short c = triangulos.get(j+2);
			
			arrayVertices[i] = vertices.get(2*a);
			arrayVertices[i+1] = vertices.get(2*a+1);
			
			arrayVertices[i+2] = vertices.get(2*b);
			arrayVertices[i+3] = vertices.get(2*b+1);
			
			arrayVertices[i+4] = vertices.get(2*b);
			arrayVertices[i+5] = vertices.get(2*b+1);
			
			arrayVertices[i+6] = vertices.get(2*c);
			arrayVertices[i+7] = vertices.get(2*c+1);		
			
			arrayVertices[i+8] = vertices.get(2*c);
			arrayVertices[i+9] = vertices.get(2*c+1);
			
			arrayVertices[i+10] = vertices.get(2*a);
			arrayVertices[i+11] = vertices.get(2*a+1);
			
			j = j+3;
			i = i+12;
		}	
		
		return construirBufferListaPuntos(arrayVertices);
	}
	
	// Construcci�n de un buffer de pintura para lineas a partir de una lista de triangulos.
	// Uso para GL_TRIANGLES
	protected FloatBuffer construirBufferListaTriangulosRellenos(ShortArray triangulos, FloatArray vertices)
	{
		int arrayLong = triangulos.size;
		float[] arrayVertices = new float[2*arrayLong];
		
		int j = 0;
		int i = 0;
		while(j < triangulos.size)
		{
			short a = triangulos.get(j);
			short b = triangulos.get(j+1);
			short c = triangulos.get(j+2);
			
			arrayVertices[i] = vertices.get(2*a);
			arrayVertices[i+1] = vertices.get(2*a+1);
			
			arrayVertices[i+2] = vertices.get(2*b);
			arrayVertices[i+3] = vertices.get(2*b+1);
			
			arrayVertices[i+4] = vertices.get(2*c);
			arrayVertices[i+5] = vertices.get(2*c+1);
			
			j = j+3;
			i = i+6;
		}	
		
		return construirBufferListaPuntos(arrayVertices);
	}
	
	// Construcci�n de Coordenadas de Textura a partir de una lista de puntos.
	protected FloatArray construirTextura(FloatArray puntos, float width, float height)
	{
		FloatArray textura = new FloatArray(puntos.size);
		
		int i = 0;
		while(i < puntos.size)
		{
			float x = puntos.get(i);
			float y = puntos.get(i+1);
			
			// Conversi�n a Pixeles
			float px = (x - xLeft)*width/(xRight-xLeft);
			float py = (y - yBot)*height/(yTop-yBot);
			
			// Conversi�n a Coordenadas de Textura
			float cx = px / width;
			float cy = (height - py)/ height;
			
			textura.add(cx);
			textura.add(cy);
			
			i = i+2;
		}
		return textura;
	}
	
	protected FloatArray cargarTextura(GL10 gl, int indicePegatina, int[] nombreTexturas, int posPegatina)
	{        
		Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), indicePegatina);
		
		float height = bitmap.getHeight()/2;
		float width = bitmap.getWidth()/2;
		
		cargarTextura(gl, bitmap, nombreTexturas, posPegatina);
		bitmap.recycle();
		
		FloatArray puntos = new FloatArray();
		puntos.add(-width);	puntos.add(-height);
		puntos.add(-width);	puntos.add(height);
		puntos.add(width);	puntos.add(-height);
		puntos.add(width);	puntos.add(height);	
		
		return puntos;
	}
	
	/* Metodos de Actualizaci�n de Buffers de Pintura */
	
	// Actualiza los valores de un buffer de pintura para puntos
	protected void actualizarBufferListaPuntos(FloatBuffer buffer, FloatArray vertices)
	{
		float[] arrayVertices = new float[vertices.size];
		System.arraycopy(vertices.items, 0, arrayVertices, 0, vertices.size);

		buffer.put(arrayVertices);
		buffer.position(0);
	}
	
	// Actualiza los valores de un buffer de pintura para triangulos
	protected void actualizarBufferListaTriangulosRellenos(FloatBuffer buffer, ShortArray triangulos, FloatArray vertices)
	{
		int j = 0;
		int i = 0;
		while(j < triangulos.size)
		{
			short a = triangulos.get(j);
			short b = triangulos.get(j+1);
			short c = triangulos.get(j+2);
			
			buffer.put(i, vertices.get(2*a));
			buffer.put(i+1, vertices.get(2*a+1));
			
			buffer.put(i+2, vertices.get(2*b));
			buffer.put(i+3, vertices.get(2*b+1));
			
			buffer.put(i+4, vertices.get(2*c));
			buffer.put(i+5, vertices.get(2*c+1));
			
			j = j+3;
			i = i+6;
		}
	}
	
	// Actualiza los valores de un buffer de pintura para indice puntos
	protected void actualizarBufferListaIndicePuntos(FloatBuffer buffer, ShortArray contorno, FloatArray vertices)
	{
		int j = 0;
		while(j < contorno.size)
		{
			short a = contorno.get(j);
			
			buffer.put(2*j, vertices.get(2*a));
			buffer.put(2*j+1, vertices.get(2*a+1));
			
			j++;
		}
	}

	/* M�todos de Pintura en la Tuber�a Gr�fica */
	/*	
		____________________________________
		|			|___________|			| B
		|			|			|			|
		|			|			|			| A
		|			|			|			|
		|			|___________|			|
		|___________|___________|___________| B
			recA		recB			C
	*/
	
	private void actualizarMarcos()
	{
		float height = yTop - yBot;
		float width = xRight - xLeft;
		
		marcoA = 0.8f * height;
		marcoB = 0.1f * height;
		marcoC = (width - marcoA)/2;
		
		float[] recA = {0, 0, 0, height, marcoC, 0, marcoC, height};		
		recMarcoA = construirBufferListaPuntos(recA);
		
		float[] recB = {0, 0, 0, marcoB, marcoA, 0, marcoA, marcoB};
		recMarcoB = construirBufferListaPuntos(recB);
	}
	
	protected void dibujarMarco(GL10 gl)
	{
		gl.glPushMatrix();
		
			gl.glTranslatef(xLeft, yBot, 1.0f);
			
			gl.glPushMatrix();
				
				dibujarBuffer(gl, GL10.GL_TRIANGLE_STRIP, 0, Color.argb(175, 0, 0, 0), recMarcoA);
				
				gl.glTranslatef(marcoC + marcoA, 0, 0);
				dibujarBuffer(gl, GL10.GL_TRIANGLE_STRIP, 0, Color.argb(175, 0, 0, 0), recMarcoA);
			
			gl.glPopMatrix();
			
			gl.glPushMatrix();
			
				gl.glTranslatef(marcoC, 0, 0);
				dibujarBuffer(gl, GL10.GL_TRIANGLE_STRIP, 0, Color.argb(175, 0, 0, 0), recMarcoB);
				
				gl.glTranslatef(0, marcoB + marcoA, 0);
				dibujarBuffer(gl, GL10.GL_TRIANGLE_STRIP, 0, Color.argb(175, 0, 0, 0), recMarcoB);
			
			gl.glPopMatrix();
			
		gl.glPopMatrix();
	}
	
	// Pintura de un Buffer de Puntos
	protected void dibujarBuffer(GL10 gl, int type, int size, int color, FloatBuffer bufferPuntos)
	{	
		gl.glColor4f(Color.red(color)/255.0f, Color.green(color)/255.0f, Color.blue(color)/255.0f, Color.alpha(color)/255.0f);
		gl.glFrontFace(GL10.GL_CW);
		
		if(type == GL10.GL_POINTS)
		{
			gl.glPointSize(size);
		}
		else {
			gl.glLineWidth(size);
		}
		
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, bufferPuntos);
		
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glDrawArrays(type, 0, bufferPuntos.capacity() / 2);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}
	
	// Pintura de una Lista de Handles
	protected void dibujarListaIndiceHandle(GL10 gl, int color, FloatBuffer handle, FloatArray posiciones)
	{
		gl.glPushMatrix();
		
			int i = 0;
			while(i < posiciones.size)
			{
				float estado = posiciones.get(i+1);
				
				if(estado == 1)
				{
					float x = posiciones.get(i+2);
					float y = posiciones.get(i+3);
					float z = 0.0f;
					
					gl.glPushMatrix();
						gl.glTranslatef(x, y, z);
						dibujarBuffer(gl, GL10.GL_TRIANGLE_FAN, SIZELINE, color, handle);
					gl.glPopMatrix();
				}
				
				i = i+4;
			}
		
		gl.glPopMatrix();
	}
	
	// Pintura de una Lista de Handles
	protected void dibujarListaHandle(GL10 gl, int color, FloatBuffer handle, FloatArray posiciones)
	{
		gl.glPushMatrix();
		
			int i = 0;
			while(i < posiciones.size)
			{
				float x = posiciones.get(i);
				float y = posiciones.get(i+1);
				float z = 0.0f;
					
				gl.glPushMatrix();
					gl.glTranslatef(x, y, z);
					dibujarBuffer(gl, GL10.GL_TRIANGLE_FAN, SIZELINE, color, handle);
				gl.glPopMatrix();
				
				i = i+2;
			}
		
		gl.glPopMatrix();
	}
	
	/* TESTING */
	protected void dibujarListaHandleMultitouch(GL10 gl, FloatBuffer handle, FloatArray posiciones)
	{
		gl.glPushMatrix();
		
		int i = 0;
		while(i < posiciones.size)
		{
			float estado = posiciones.get(i);
			
			// estado = 0 SUELTO
			// estado = 1 PULSADO
			if(estado == 1)
			{
				float x = posiciones.get(i+1);
				float y = posiciones.get(i+2);
				float z = 0.0f;
				
				int color = Color.BLACK;
				switch(i/3)
				{
					case 0:
						color = Color.BLUE;
					break;
					case 1:
						color = Color.YELLOW;
					break;
					case 2:
						color = Color.RED;
					break;
					case 3:
						color = Color.GREEN;
					break;
				}
				
				gl.glPushMatrix();
					gl.glTranslatef(x, y, z);
					dibujarBuffer(gl, GL10.GL_TRIANGLE_FAN, SIZELINE, color, handle);
				gl.glPopMatrix();
			}
			
			i = i+3;
		}
	
		gl.glPopMatrix();
	}
	/* TESTING */
	
	// Cargado de Textura
	protected void cargarTextura(GL10 gl, Bitmap textura, int[] nombreTextura, int pos)
	{
		gl.glEnable(GL10.GL_TEXTURE_2D);
			
			gl.glGenTextures(1, nombreTextura, pos);
			gl.glBindTexture(GL10.GL_TEXTURE_2D, nombreTextura[pos]);
			
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, textura, 0);
		
		gl.glDisable(GL10.GL_TEXTURE_2D);
	}
	
	// Dibujar Textura para una Lista de Puntos asociada a una Lista de Coordenadas de Textura
	private void dibujarTextura(GL10 gl, int type, FloatBuffer bufferPuntos, FloatBuffer bufferCoordTextura, int[] nombreTextura, int posTextura)
	{
		gl.glEnable(GL10.GL_TEXTURE_2D);
		
			gl.glBindTexture(GL10.GL_TEXTURE_2D, nombreTextura[posTextura]);
			gl.glFrontFace(GL10.GL_CW);
			gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			
			gl.glVertexPointer(2, GL10.GL_FLOAT, 0, bufferPuntos);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, bufferCoordTextura);
			
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
				gl.glDrawArrays(type, 0, bufferPuntos.capacity()/2);
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		
		gl.glDisable(GL10.GL_TEXTURE_2D);
	}
	
	protected void dibujarTextura(GL10 gl, FloatBuffer bufferPuntos, FloatBuffer bufferCoordTextura, int[] nombreTextura, int posTextura)
	{
		dibujarTextura(gl, GL10.GL_TRIANGLES, bufferPuntos, bufferCoordTextura, nombreTextura, posTextura);
	}
	
	protected void dibujarPegatina(GL10 gl, FloatBuffer bufferPuntos, FloatBuffer bufferCoordTextura, float x, float y, int[] nombreTextura, int posTextura)
	{
		gl.glPushMatrix();
		
			gl.glTranslatef(x, y, 0.0f);
			
			dibujarTextura(gl, GL10.GL_TRIANGLE_STRIP, bufferPuntos, bufferCoordTextura, nombreTextura, posTextura);
	
		gl.glPopMatrix();
	}
	
	/* M�todos Gen�ricos */
	
	// Generar Color Aleatorio
	protected int generarColorAleatorio()
	{
		Random rand = new Random();
		
		int red = (int)(255*rand.nextFloat());
		int green = (int)(255*rand.nextFloat());
		int blue = (int)(255*rand.nextFloat());
		
		return Color.rgb(red, green, blue);
	}
}
