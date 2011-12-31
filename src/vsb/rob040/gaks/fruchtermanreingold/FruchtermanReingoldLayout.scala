package vsb.rob040.gaks.fruchtermanreingold

import scala.util.Random
import vsb.graphinterfaces._

class FruchtermanReingoldLayout(val width: Int, val height: Int, val graph: Graph, val iterations: Int) extends Layout {

	val forceConstant = 0.8 * math.sqrt(width * height / graph.getVertices.size)
	var temperature = 0.1 * math.sqrt(width * height)
	val epsilon = 0.00001D
	var currentIteration = 1
	var onLoop: (Int, Boolean) => Unit = null
	
	def getWidth: Int = width
	def getHeight: Int = height
	def getGraph: Graph = graph
	
	def shuffle = {
		val random = new Random
		graph.getVertices.foreach(vertex => {
				vertex.getMetadata.setX(random.nextInt(width))
				vertex.getMetadata.setY(random.nextInt(height))
			})
	}
	
	def run = {
		// init metadata
		graph.getVertices.foreach(initMetadata)
	
		for(iteration <- 1 to iterations) {
			
			// clear disp metadata
			graph.getVertices.foreach(vertex => vertex.getMetadata.getValue("fr").get.asInstanceOf[VertexMetadata].disp.clear)

			// repulsion
			graph.getVertices.foreach(calculateRepulsion)

			// attraction
			graph.getEdges.foreach(calculateAttraction)

			// position
			graph.getVertices.foreach(calculatePosition)
			
			// cool
			cool(iteration)
			
			// invoke callback
			if(onLoop != null)
				onLoop(iteration, iteration == iterations)
		}
	}
	
	protected def initMetadata(vertex: Vertex) = {
		val vertexMetadata = new VertexMetadata
		vertexMetadata.loc.set(vertex.getMetadata.getX, vertex.getMetadata.getY)
		vertex.getMetadata.setValue("fr", vertexMetadata)
	}
	
	protected def calculateRepulsion(vertex: Vertex) = {
		val meta = vertex.getMetadata.getValue("fr").get.asInstanceOf[VertexMetadata]
		graph.getVertices.foreach(vertex2 => {
				val meta2 = vertex2.getMetadata.getValue("fr").get.asInstanceOf[VertexMetadata]
				if(vertex != vertex2) {
					val delta = meta.loc - meta2.loc
					val deltaLength = math.max(epsilon, delta.lenght)
					val force = forceConstant * forceConstant / deltaLength
					if(force.isNaN)
						throw new IllegalStateException("Force is NaN")
					meta.disp += delta * force / deltaLength
				}
			})
	}
	
	protected def calculateAttraction(edge: Edge) = {
		val metaA = edge.getVertexA.getMetadata.getValue("fr").get.asInstanceOf[VertexMetadata]
		val metaB = edge.getVertexB.getMetadata.getValue("fr").get.asInstanceOf[VertexMetadata]

		val delta = metaA.loc - metaB.loc
		val deltaLength = math.max(epsilon, delta.lenght)
		val force = deltaLength * deltaLength / forceConstant
		if(force.isNaN)
			throw new IllegalStateException("Force is NaN")

		val disp = delta * force / deltaLength

		metaA.disp -= disp
		metaB.disp += disp
	}
	
	protected def calculatePosition(vertex: Vertex) = {
		val meta = vertex.getMetadata.getValue("fr").get.asInstanceOf[VertexMetadata]
		val delta = math.max(epsilon, meta.disp.lenght)
		val disp = meta.disp * math.min(temperature, delta) / delta
		if(disp.isNaN)
			throw new IllegalStateException("Disp is NAN")

		meta.loc.x = between(0, meta.loc.x + disp.x, width)
		meta.loc.y = between(0, meta.loc.y + disp.y, height)

		vertex.getMetadata.setX(meta.loc.x.toInt)
		vertex.getMetadata.setY(meta.loc.y.toInt)
	}
	
	protected def cool(iteration: Int) = {
		temperature *= (1.0D - (iteration.toDouble / iterations));
	}
	
	protected def between(min: Double, value: Double, max: Double): Double = {
		if(value < min)
			return min
		if(value > max)
			return max
		return value
	}
	
}

protected class Vector(var x: Double = 0.0, var y: Double = 0.0) {
	
	def +(operand: Vector): Vector = {
		return new Vector(x + operand.x, y + operand.y)
	}
	
	def -(operand: Vector): Vector = {
		return new Vector(x - operand.x, y - operand.y)
	}
	
	def *(operand: Vector): Vector = {
		return new Vector(x * operand.x, y * operand.y)
	}
	
	def *(operand: Double): Vector = {
		return new Vector(x * operand, y * operand)
	}
	
	def /(operand: Double): Vector = {
		return new Vector(x / operand, y / operand)
	}
	
	def isNaN: Boolean = x.isNaN || y.isNaN
	
	def set(x: Double, y: Double): Vector = {
		this.x = x
		this.y = y
		return this
	}
	
	def clear = {
		x = 0.0
		y = 0.0
	}
	
	def lenght = math.sqrt(x * x + y * y)
	
}

protected class VertexMetadata {
	var loc = new Vector
	var disp = new Vector
}