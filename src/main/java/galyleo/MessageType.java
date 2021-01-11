package galyleo;

/**
 * Jupyter {@link Message} types.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public enum MessageType {
    /*
     * Shell
     */
    execute_request, execute_reply,
    inspect_request, inspect_reply,
    complete_request, complete_reply,
    history_request, history_reply,
    is_complete_request, is_complete_reply,
    comm_info_request, comm_info_reply,
    kernel_info_request, kernel_info_reply,
    comm_open, comm_msg, comm_close,
    /*
     * Control
     */
    shutdown_request, shutdown_reply,
    interrupt_request, interrupt_reply,
    debug_request, debug_reply,
    /*
     * IOPub
     */
    stream,
    display_data,
    update_display_data,
    /* execute_request, execute_reply */
    error,
    status,
    clear_output,
    debug_event,
    /*
     * Stdin
     */
    input_request, input_reply;
}
