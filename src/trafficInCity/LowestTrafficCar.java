package trafficInCity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import environment.Junction;
import environment.Road;
import jade.lang.acl.ACLMessage;
import javafx.util.Pair;
import main.ContextManager;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.gis.Geography;

public class LowestTrafficCar extends Car {
	
	private int atualIndexInJunction;
	private int atualIndex;
	private List<Pair<Junction, Vector<Coordinate>>> route;

	public LowestTrafficCar(Geography<? extends AgentTraffi> space, Point finalPos) {
		super(space, finalPos);
		atualIndexInJunction = 0;
		atualIndex = 0;
		route = new ArrayList<Pair<Junction, Vector<Coordinate>>>();
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void move() {

		ACLMessage msg = receive();
		if (msg!=null) {
			System.out.println("Car: Recebi a mensagem r�dio: \"" + msg.getContent() + "\", obriado");
		}

		if (atualIndex < route.size() - 1) {

			Vector<Coordinate> coordsJuntion = route.get(atualIndex).getValue();

			if (atualIndexInJunction < coordsJuntion.size()) {

				Coordinate point = coordsJuntion.get(atualIndexInJunction);

				double lon = (point.y - actualPos().getY());
				double dx = Math.cos(actualPos().getX()) * Math.sin(point.x)
						- Math.sin(actualPos().getX()) * Math.cos(point.x) * Math.cos(lon);
				double dy = Math.sin(lon) * Math.cos(point.y);
				double ang = Math.atan2(dy, dx);

				ang = ((Math.PI * 2) + ang) % (Math.PI * 2);
				ang = (Math.PI * 2) - ang;

				// System.out.println(ang);
				ContextManager.moveAgentByVector(this, 0.0001 * ContextManager.agentTraffiContext.size(), ang);

				int angl = (int) (ang * 10000);
				int pi = (int) (Math.PI * 10000);

				if ((angl == pi) || angl == 2 * pi || angl == (int) (pi / 2) || angl == (int) (3 * pi / 2)) {

					if (route.get(atualIndex).getValue().size() == atualIndexInJunction + 1) {

						Junction oldsourceJunction = route.get(atualIndex).getKey();
						Junction oldtargetJunction = route.get(atualIndex + 1).getKey();
						
						Iterator<AgentTraffi> radios = ContextManager.agentTraffiContext.getObjects(Radio.class).iterator();
						
						if(radios.hasNext()) {
							Radio radio = (Radio)radios.next();
							radio.subIndexjunctionsCars(new Pair<Junction,Junction> (oldsourceJunction,oldtargetJunction));
						}
						
						atualIndex++;
						atualIndexInJunction = 0;

						if(atualIndex < route.size() - 1 ) {
							Junction newsourceJunction = route.get(atualIndex).getKey();
							Junction newtargetJunction = route.get(atualIndex +1).getKey();
							
							Iterator<AgentTraffi> radiosAtual = ContextManager.agentTraffiContext.getObjects(Radio.class).iterator();
							
							if(radiosAtual.hasNext()) {
								Radio radioAtual = (Radio)radiosAtual.next();
								radioAtual.addIndexjunctionsCars(new Pair<Junction,Junction> (newsourceJunction,newtargetJunction));
							}
						}
					} else
						atualIndexInJunction++;

				}
			}
		}
	}
	
	public void runDiskj() {



		Coordinate i = new Coordinate(actualPos().getX(), actualPos().getY());
		Coordinate f = new Coordinate(finalPos.getX(), finalPos.getY());

		if (i.equals(f))
			return;

		Junction actJunction = ContextManager.getJunction(i);
		Junction finalJunction = ContextManager.getJunction(f);



		PriorityQueue<Junction> queue = new PriorityQueue<Junction>(1000);

		ArrayList<Junction> shortestPathList = new ArrayList<Junction>();

		resetWeightInJunctions();

		actJunction.setNodeWeight(0);

		queue.add(actJunction);
		while (!queue.isEmpty()) {
			Junction j = queue.poll();
			Iterator<Junction> successors = ContextManager.streetNetwork.getSuccessors(j).iterator();

			while (successors.hasNext()) {
				Junction n = successors.next();

				double newWeight = j.getNodeWeight() + getRoadWeight(j, n);

				if (n.getNodeWeight() > newWeight) {
					n.setNodeWeight(newWeight);
					n.setPreviousNode(j);
					queue.add(n);

				}
			}
		}

		Junction crawler = finalJunction;
		while (!crawler.equals(actJunction)) {
			shortestPathList.add(crawler.getPreviousNode());
			crawler = crawler.getPreviousNode();
		}

		Collections.reverse(shortestPathList);
		defineRoute(shortestPathList);

	}

	public double getRoadWeight(Junction n1, Junction n2) {
		List<Road> connections = n1.getRoads();
		for (int i = 0; i < connections.size(); i++) {
			Road road = connections.get(i);
			ArrayList<Junction> current = road.getJunctions();

			if (current.contains(n1) && current.contains(n2)) {
				return road.getEdge().getWeight();
			}
		}
		return Double.MAX_VALUE;
	}

	public void resetWeightInJunctions() {
		Iterator<Junction> junctions = ContextManager.junctionContext.getObjects(Junction.class).iterator();

		while (junctions.hasNext()) {
			Junction current = junctions.next();
			current.setNodeWeight(Double.MAX_VALUE);
		}
	}

	public void defineRoute(List<Junction> junctions) {

		Iterator<AgentTraffi> radios = ContextManager.agentTraffiContext.getObjects(Radio.class).iterator();
		
		if(radios.hasNext() && junctions.size() >= 2) {
			Radio radio = (Radio)radios.next();
			radio.subIndexjunctionsCars(new Pair<Junction,Junction> (junctions.get(0), junctions.get(1)));
		}
		
		for (int i = 1; i < junctions.size(); i++) {
			Vector<Coordinate> coordsRoad = new Vector<Coordinate>();
			Iterator<Road> roads = ContextManager.roadContext.getObjects(Road.class).iterator();
			Road r = null;
			Junction sourceJunction = junctions.get(i - 1);
			Junction targetJunction = junctions.get(i);

			while (roads.hasNext()) {
				r = roads.next();
				if (r.getJunctions().contains(sourceJunction) && r.getJunctions().contains(targetJunction)) {
					break;
				}
			}
			Coordinate[] coords = ContextManager.roadProjection.getGeometry(r).getCoordinates();
			if (coords[0].equals(junctions.get(i - 1).getCoords())) {
				for (int j = 0; j < coords.length; j++) {
					coordsRoad.addElement(coords[j]);
				}
			} else {
				for (int j = coords.length - 1; j >= 0; j--) {
					coordsRoad.addElement(coords[j]);
				}
			}
			route.add(new Pair<Junction, Vector<Coordinate>>(sourceJunction, coordsRoad));
		}
		route.add(
				new Pair<Junction, Vector<Coordinate>>(junctions.get(junctions.size() - 1), new Vector<Coordinate>()));
	}
}
