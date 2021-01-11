package feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;

import ar.com.smg.bpm.atencioncic.MockRestWorkItemHandler;
import io.cucumber.java.Before;
import io.cucumber.java.ParameterType;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;

public class AtencionCICStep extends JbpmJUnitBaseTestCase {
	
	private RuntimeManager manager;
	private RuntimeEngine engine;
	private KieSession ksession;
	private TaskService taskService;
	
	private List<UserTask> userTasks;
	
	private String actor;
	
	private MockRestWorkItemHandler restWorkItemHandler = new MockRestWorkItemHandler();
	
    @ParameterType("(?:[^,]*)(?:,\\s?[^,]*)*")
    public String[] listOfStrings(String arg){
        return arg.split(",\\s?");
    }

	@Entonces("verificar el flujo de ejecución sea el siguiente: Inicio -> \"{listOfStrings}\" -> Fin")
	public void verificar_el_flujo_de_ejecución_sea_el_siguiente(String[] steps) throws InterruptedException {
		try {
			
			ksession.getWorkItemManager().registerWorkItemHandler("Rest", restWorkItemHandler);
			
			// start a new process instance
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("businessBody", "{\"atencionCIC\": {\"detalle\": {\"sucursal\": {\"id\": 7,\"ambito\": \"AMB_SUC_ARENALES\"},\"altaUsuario\": {\"id\": \"CN=Micaela Achilli,OU=Externos,OU=Groups&Users,OU=-SMG-,DC=swm,DC=com,DC=ar\",\"login\": \"miachilli\"},\"motivo\": {\"id\": 17},\"submotivo\": {\"id\": 1},\"observacion\":\"dsada\"},\"afiliado\": {\"contra\": \"0327220\",\"inte\": \"02\",\"prepaga\": 0},\"medio\": {\"id\": 1,\"valor\": \"\"}},\"process\": {\"processName\": \"Atencion CIC\",\"variables\": {\"processInfo\": {\"procesoRelacionadoId\": null}}}}");
			ProcessInstance processInstance = ksession.startProcess("swiss.Atención-CIC", params);
	        System.out.println("Process started ..."); 

	        for (UserTask ut : userTasks) {
				ut.complete();
			}

			assertProcessInstanceActive(processInstance.getId(), ksession);
			
	    	assertNodeTriggered(processInstance.getId(), steps);

			System.out.println("Process instance completed");

		} finally {
			manager.disposeRuntimeEngine(engine);
			manager.close();			
		}
		
	}
	
	@Dado("tomo la tarea en estado {string} y la transiciono al estado {string}")
	public void tomo_la_tarea_en_estado_y_la_transiciono_al_estado(String string, String dest) {
		Map<String, Object> results = new HashMap<String, Object>();
		results.put("resolverTaskDecition", dest);
		UserTask ut = buildUserTask(actor, results);
		userTasks.add(ut);
	}
	
	@Dado("falla la Inserción de datos en el CRM")
	public void falla_la_inserción_de_datos_en_el_crm() {
		restWorkItemHandler = new MockRestWorkItemHandler(4);
	}

	@Dado("que soy {string} y pertenezco al grupo {string}")
	public void que_soy_y_pertenezco_al_grupo(String name, String group) {
	    this.actor = name;
	}
	
	@Before
	public void before() throws Exception{
        super.setupDataSource = true;
        super.sessionPersistence = true;
        super.setupPoolingDataSource();
        
		manager = createRuntimeManager("ar/com/smg/bpm/atencioncic.bpmn","ar/com/smg/bpm/atencioncic/subprocess-rest-event.bpmn");
		engine = getRuntimeEngine(null);
		ksession = engine.getKieSession();
		taskService = engine.getTaskService();
		
		userTasks = new ArrayList<UserTask>();
	}

	private UserTask buildUserTask(String actor, Map<String, Object> results) {
		UserTask ut = new UserTask() {
			@Override
			public void complete() {
				List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(actor, "en-UK");
//				assertEquals(1, tasks.size());
				TaskSummary task = tasks.get(0);
				System.out.println(actor + " completing task " + task.getName() + ": " + task.getDescription());
				taskService.start(task.getId(), actor);
				taskService.complete(task.getId(), actor, results);
			}
		};
		return ut;
	}
}

interface UserTask {
	public void complete();
}