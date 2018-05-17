/*
 *
 *
 * Distributed under the QNAP License.
 * Developed by Susmit Basu
 * 
 * 
 */

import DDS.*;
import OpenDDS.DCPS.*;
import org.omg.CORBA.StringSeqHolder;
import Messenger.*; // Change if project changes

public class TestSubscriber extends DDS._DataReaderListenerLocalBase { // Change if project changes
    // For clean shutdown sequence
    public static String DDS_TOPIC_NAME = "DDS DEVICE COMM"; 
    private static boolean shutdown_flag = false;
    public static boolean checkReliable(String[] args) {
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-r")) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws Exception {

        System.out.println("Start Subscriber");
        boolean reliable = checkReliable(args);

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
        Topic top = dp.create_topic(DDS_TOPIC_NAME, servant.get_type_name(),
                TOPIC_QOS_DEFAULT.get(), null, DEFAULT_STATUS_MASK.value);
        if (top == null) {
            System.err.println("ERROR: Topic creation failed");
            return;
        }

        Subscriber sub = dp.create_subscriber(SUBSCRIBER_QOS_DEFAULT.get(),
                null, DEFAULT_STATUS_MASK.value);
        if (sub == null) {
            System.err.println("ERROR: Subscriber creation failed");
            return;
        }

        // Use the default transport (do nothing)

        DataReaderQos dr_qos = new DataReaderQos();
        dr_qos.durability = new DurabilityQosPolicy();
        dr_qos.durability.kind = DurabilityQosPolicyKind.from_int(0);
        dr_qos.deadline = new DeadlineQosPolicy();
        dr_qos.deadline.period = new Duration_t();
        dr_qos.latency_budget = new LatencyBudgetQosPolicy();
        dr_qos.latency_budget.duration = new Duration_t();
        dr_qos.liveliness = new LivelinessQosPolicy();
        dr_qos.liveliness.kind = LivelinessQosPolicyKind.from_int(0);
        dr_qos.liveliness.lease_duration = new Duration_t();
        dr_qos.reliability = new ReliabilityQosPolicy();
        dr_qos.reliability.kind = ReliabilityQosPolicyKind.from_int(0);
        dr_qos.reliability.max_blocking_time = new Duration_t();
        dr_qos.destination_order = new DestinationOrderQosPolicy();
        dr_qos.destination_order.kind = DestinationOrderQosPolicyKind
                .from_int(0);
        dr_qos.history = new HistoryQosPolicy();
        dr_qos.history.kind = HistoryQosPolicyKind.from_int(0);
        dr_qos.resource_limits = new ResourceLimitsQosPolicy();
        dr_qos.user_data = new UserDataQosPolicy();
        dr_qos.user_data.value = new byte[0];
        dr_qos.ownership = new OwnershipQosPolicy();
        dr_qos.ownership.kind = OwnershipQosPolicyKind.from_int(0);
        dr_qos.time_based_filter = new TimeBasedFilterQosPolicy();
        dr_qos.time_based_filter.minimum_separation = new Duration_t();
        dr_qos.reader_data_lifecycle = new ReaderDataLifecycleQosPolicy();
        dr_qos.reader_data_lifecycle.autopurge_nowriter_samples_delay = new Duration_t();
        dr_qos.reader_data_lifecycle.autopurge_disposed_samples_delay = new Duration_t();

        DataReaderQosHolder qosh = new DataReaderQosHolder(dr_qos);
        sub.get_default_datareader_qos(qosh);
        if (reliable) {
            qosh.value.reliability.kind = ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;
        }
        qosh.value.history.kind = HistoryQosPolicyKind.KEEP_ALL_HISTORY_QOS;
        TestSubscriber listener = new TestSubscriber(); // Change if project changes
        DataReader dr = sub.create_datareader(top, qosh.value, listener,
                DEFAULT_STATUS_MASK.value);
        if (dr == null) {
            System.err.println("ERROR: DataReader creation failed");
            return;
        }
        System.out.println("Ready to read data.");
        System.out.println("Press CTRL+C to terminate.");

        for (;;) {
            try {
                Thread.sleep(1000);
                if (shutdown_flag)
                    break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        StatusCondition sc = dr.get_statuscondition();
        sc.set_enabled_statuses(SUBSCRIPTION_MATCHED_STATUS.value);
        WaitSet ws = new WaitSet();
        ws.attach_condition(sc);
        SubscriptionMatchedStatusHolder matched = new SubscriptionMatchedStatusHolder(
                new SubscriptionMatchedStatus());
        Duration_t timeout = new Duration_t(DURATION_INFINITE_SEC.value,
                DURATION_INFINITE_NSEC.value);

        System.out.println("\n DURATION_INFINITE_SEC.value ["
                + DURATION_INFINITE_SEC.value);
        System.out.println("\n DURATION_INFINITE_NSEC.value ["
                + DURATION_INFINITE_NSEC.value);
        boolean matched_pub = false;
        while (true) {
            final int result = dr.get_subscription_matched_status(matched);
            if (result != RETCODE_OK.value) {
                System.err.println("ERROR: get_subscription_matched_status()"
                        + "failed.");
                return;
            }

            if (matched.value.current_count == 0
                    && matched.value.total_count > 0) {
                System.out.println("Subscriber No Longer Matched");
                break;
            } else if (matched.value.current_count > 0 && !matched_pub) {
                System.out.println("Subscriber Matched");
                matched_pub = true;
            }

            ConditionSeqHolder cond = new ConditionSeqHolder(new Condition[] {});
            if (ws.wait(cond, timeout) != RETCODE_OK.value) {
                System.err.println("ERROR: wait() failed.");
                return;
            }
        }

        System.out.println("Subscriber Report Validity");
        // listener.report_validity();
        ws.detach_condition(sc);
        System.out.println("Stop Subscriber");
        dp.delete_contained_entities();
        dpf.delete_participant(dp);
        TheServiceParticipant.shutdown();
        System.out.println("Subscriber exiting");
    }

    public void on_data_available(DDS.DataReader reader) {
        
        MessageDataReader mdr = MessageDataReaderHelper.narrow(reader); // Change if project changes
        if (mdr == null) {
            System.err.println("ERROR: read: narrow failed.");
            return;
        }

        MessageHolder mh = new MessageHolder(new Message());  // Change if project changes
        SampleInfoHolder sih = new SampleInfoHolder(new SampleInfo(0, 0, 0,
                new DDS.Time_t(), 0, 0, 0, 0, 0, 0, 0, false, 0));      
        for (;;) {
        int status = mdr.take_next_sample(mh, sih);
            if (status == RETCODE_OK.value) {
                if (sih.value.valid_data) {
                    System.out.println(" - Command received as  ->  -count-  "
                            + mh.value.count + "  -Command-  " + mh.value.text);
                    if (sih.equals("")) {
                        shutdown_flag = true;
                        break;
                    }
                }
                break;
            } else if (sih.value.instance_state == NOT_ALIVE_DISPOSED_INSTANCE_STATE.value) {
                System.out.println("instance is disposed");
                shutdown_flag = true;
                break;
            } else if (sih.value.instance_state == NOT_ALIVE_NO_WRITERS_INSTANCE_STATE.value) {
                System.out.println("instance is unregistered");
                shutdown_flag = true;
                break;
            } else {
                System.out
                        .println("DataReaderListenerImpl::on_data_available: "
                                + "ERROR: received unknown instance state "
                                + sih.value.instance_state);
                shutdown_flag = true;
                break;
            }

        }

    }

    public void on_requested_deadline_missed(DDS.DataReader reader,
            DDS.RequestedDeadlineMissedStatus status) {
        System.err
                .println("DataReaderListenerImpl.on_requested_deadline_missed");
    }

    public void on_requested_incompatible_qos(DDS.DataReader reader,
            DDS.RequestedIncompatibleQosStatus status) {
        System.err
                .println("DataReaderListenerImpl.on_requested_incompatible_qos");
    }

    public void on_sample_rejected(DDS.DataReader reader,
            DDS.SampleRejectedStatus status) {
        System.err.println("DataReaderListenerImpl.on_sample_rejected");
    }

    public void on_liveliness_changed(DDS.DataReader reader,
            DDS.LivelinessChangedStatus status) {
        System.err.println("DataReaderListenerImpl.on_liveliness_changed");
    }

    public void on_subscription_matched(DDS.DataReader reader,
            DDS.SubscriptionMatchedStatus status) {
        System.err.println("DataReaderListenerImpl.on_subscription_matched");
    }

    public void on_sample_lost(DDS.DataReader reader,
            DDS.SampleLostStatus status) {
        System.err.println("DataReaderListenerImpl.on_sample_lost");
    }

}
