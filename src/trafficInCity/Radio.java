package trafficInCity;

import java.util.Iterator;

import environment.Junction;
import environment.Road;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import main.ContextManager;
import repast.simphony.engine.schedule.ScheduledMethod;


public class Radio extends AgentTraffi{
	private RoadTrafficIntensity carTrafficInfo;
	private boolean a;

	public Radio () {
		carTrafficInfo = new RoadTrafficIntensity();
		a = true;
	}

	@ScheduledMethod(start = 1, interval = 1)
	public void updateAllRoadWeight() {
		Iterator<Road> roads = ContextManager.roadContext.getObjects(Road.class).iterator();

		int load;

		while(roads.hasNext()) {
			load = 3;

			Road current =  roads.next();
			Junction source = current.getJunctions().get(0);
			Junction target = current.getJunctions().get(1);

			/*
		      load = carTrafficInfo.getRoadLoad(new Pair<Junction, Junction>(source, target));
		      load = carTrafficInfo.getRoadLoad(new Pair<Junction, Junction>(source, target));
			 */
			current.setLoad(load);
			
			if(a) {
				Iterator<AgentTraffi> cars = ContextManager.agentTraffiContext.getObjects(Car.class).iterator();
				String msg = "Oi, sou o radio";
				System.out.println("Radio: " + msg);
				
				while(cars.hasNext()) {
					Car c = (Car) cars.next();
					
					AID receiver = (AID) c.getAID();
					sendMessage(receiver, msg);
				}
				a = false;
			}
		}
	}

	public void sendMessage(AID car, String message) {
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setContent(message);
		msg.addReceiver(car);
		send(msg);
	}
}