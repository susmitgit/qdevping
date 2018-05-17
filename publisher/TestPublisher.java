/*
 *
 *
 * Developed by Darshan Khendake
 * 
 * 
 */
import DDS.*;
import Messenger.*; // Change if project changes
import OpenDDS.DCPS.*;

import org.omg.CORBA.StringSeqHolder;

public class TestPublisher { // Change if project changes
    public static String DDS_TOPIC_NAME = "DDS DEVICE COMM"; 
    public static boolean checkReliable(String[] args) {
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-r")) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkWaitForAcks(String[] args) {
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-w")) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws InterruptedException {

        System.out.println("Start Publisher");
        boolean reliable = checkReliable(args);
        boolean waitForAcks = checkWaitForAcks(args);
        String msgPubDelay;
        if(args[0] == null){            
            msgPubDelay = "1000";           
        }else{
            msgPubDelay= args[0];
        }
        System.out.println("Message publish delay time "+ msgPubDelay + " ms");
        DomainParticipantFactory dpf = TheParticipantFactory
                .WithArgs(new StringSeqHolder(args));
        if (dpf == null) {
            System.err.println("ERROR: Domain Participant Factory not found");
            return;
        }
        DomainParticipant dp = dpf.create_participant(4,
                PARTICIPANT_QOS_DEFAULT.get(), null, DEFAULT_STATUS_MASK.value);
        if (dp == null) {
            System.err.println("ERROR: Domain Participant creation failed");
            return;
        }

        MessageTypeSupportImpl servant = new MessageTypeSupportImpl(); // Change if project changes
        if (servant.register_type(dp, "") != RETCODE_OK.value) {
            System.err.println("ERROR: register_type failed");
            return;
        }

        Topic top = dp.create_topic(DDS_TOPIC_NAME,
                servant.get_type_name(), TOPIC_QOS_DEFAULT.get(), null,
                DEFAULT_STATUS_MASK.value);
        if (top == null) {
            System.err.println("ERROR: Topic creation failed");
            return;
        }

        // Use the default transport configuration (do nothing)

        DataWriterQos dw_qos = new DataWriterQos();
        dw_qos.durability = new DurabilityQosPolicy();
        dw_qos.durability.kind = DurabilityQosPolicyKind.from_int(0);
        dw_qos.durability_service = new DurabilityServiceQosPolicy();
        dw_qos.durability_service.history_kind = HistoryQosPolicyKind
                .from_int(0);
        dw_qos.durability_service.service_cleanup_delay = new Duration_t();
        dw_qos.deadline = new DeadlineQosPolicy();
        dw_qos.deadline.period = new Duration_t();
        dw_qos.latency_budget = new LatencyBudgetQosPolicy();
        dw_qos.latency_budget.duration = new Duration_t();
        dw_qos.liveliness = new LivelinessQosPolicy();
        dw_qos.liveliness.kind = LivelinessQosPolicyKind.from_int(0);
        dw_qos.liveliness.lease_duration = new Duration_t();
        dw_qos.reliability = new ReliabilityQosPolicy();
        dw_qos.reliability.kind = ReliabilityQosPolicyKind.from_int(0);
        dw_qos.reliability.max_blocking_time = new Duration_t();
        dw_qos.destination_order = new DestinationOrderQosPolicy();
        dw_qos.destination_order.kind = DestinationOrderQosPolicyKind
                .from_int(0);
        dw_qos.history = new HistoryQosPolicy();
        dw_qos.history.kind = HistoryQosPolicyKind.from_int(0);
        dw_qos.resource_limits = new ResourceLimitsQosPolicy();
        dw_qos.transport_priority = new TransportPriorityQosPolicy();
        dw_qos.lifespan = new LifespanQosPolicy();
        dw_qos.lifespan.duration = new Duration_t();
        dw_qos.user_data = new UserDataQosPolicy();
        dw_qos.user_data.value = new byte[0];
        dw_qos.ownership = new OwnershipQosPolicy();
        dw_qos.ownership.kind = OwnershipQosPolicyKind.from_int(0);
        dw_qos.ownership_strength = new OwnershipStrengthQosPolicy();
        dw_qos.writer_data_lifecycle = new WriterDataLifecycleQosPolicy();
        // test

        DataWriterQosHolder qosh = new DataWriterQosHolder(dw_qos);

        Publisher pub = dp.create_publisher(PUBLISHER_QOS_DEFAULT.get(), null,
                DEFAULT_STATUS_MASK.value);
        if (pub == null) {
            System.err.println("ERROR: Publisher creation failed");
            return;
        }

        pub.get_default_datawriter_qos(qosh);
        qosh.value.history.kind = HistoryQosPolicyKind.KEEP_ALL_HISTORY_QOS;
        if (reliable) {
            qosh.value.reliability.kind = ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;
        }

        DataWriter dw = pub.create_datawriter(top, qosh.value, null,
                DEFAULT_STATUS_MASK.value);
        if (dw == null) {
            System.err.println("ERROR: DataWriter creation failed");
            return;
        }
        System.out.println("Publisher Created DataWriter");
        MessageDataWriter mdw = MessageDataWriterHelper.narrow(dw); // Change if project changes
        Message msg = new Message(); // Change if project changes
        
        try {
            int loopCount = 0;
            while (true) {
                msg.count = loopCount;
                msg.text = "Current Message count and Command ID  :  ->  exec('Light LED')   - > Count   "
                        + msg.count;// reader.readLine();
                if (msg.text == null)
                    break; // shouldn't happen
                int handle = mdw.register_instance(msg);
                mdw.write(msg, handle);
                if (msg.text.equals(""))
                    break;
                Thread.sleep(Integer.parseInt(msgPubDelay));
                loopCount++;
            }
        } catch (Exception e) {
            // This exception can be thrown from DDS write operation
            e.printStackTrace();
        }

        if (waitForAcks) {
            System.out.println("Publisher waiting for acks");
            // Wait for acknowledgements
            Duration_t forever = new Duration_t(DURATION_INFINITE_SEC.value,
                    DURATION_INFINITE_NSEC.value);
            dw.wait_for_acknowledgments(forever);
        } else {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
            }
        }
        System.out.println("Stop Publisher");

        // Clean up
        dp.delete_contained_entities();
        dpf.delete_participant(dp);
        TheServiceParticipant.shutdown();
        System.out.println("Publisher exiting");
    }
}
