package ganymede.server;
/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2021, 2022 Allen D. Ball
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ##########################################################################
 */
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.zeromq.ZMQ;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Jupyter {@link ZMQ.Socket} {@link Dispatcher}.  All {@link ZMQ.Socket}
 * creation and manipulation calls happens in the {@link #run()} method.
 * See {@link.uri https://zguide.zeromq.org/ target=newtab ØMQ - The Guide},
 * {@link.uri https://zguide.zeromq.org/docs/chapter3/ target=newtab Chapter 3}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@Data @Log4j2
public class Dispatcher implements Runnable {
    @NonNull private final Channel channel;
    @NonNull private final Connection connection;
    @NonNull private final String address;
    private final BlockingQueue<Message> outgoing = new SynchronousQueue<>();

    /**
     * Callback method to dispatch a received message.  Default
     * implementation calls
     * {@link Channel#dispatch(Dispatcher,ZMQ.Socket,byte[])}.
     *
     * @param   socket          The {@link ZMQ.Socket}.
     * @param   frame           The first message frame.
     */
    protected void dispatch(ZMQ.Socket socket, byte[] frame) {
        getChannel().dispatch(this, socket, frame);
    }

    /**
     * Callback method to dispatch a {@link Message}.  Default
     * implementation calls
     * {@link Channel.Protocol#dispatch(Dispatcher,ZMQ.Socket,Message)}.
     *
     * @param   socket          The {@link ZMQ.Socket}.
     * @param   message         The {@link Message}.
     */
    protected void dispatch(ZMQ.Socket socket, Message message) {
        ((Channel.Protocol) getChannel()).dispatch(this, socket, message);
    }

    /**
     * Method to schedule a message for publishing.
     *
     * @param   message         The message to send.
     */
    public void pub(Message message) {
        var type = getChannel().getSocketType();

        switch (type) {
        case PUB:
            try {
                outgoing.put(message);
            } catch (InterruptedException exception) {
                log.warn("{}", exception);
            }
            break;

        default:
            throw new IllegalStateException("Unsupported SocketType: " + type);
        }
    }

    @Override
    public void run() {
        var server = getChannel().getServer();
        var context = server.getContext();
        var digester = getConnection().getDigester();
        var type = getChannel().getSocketType();

        while (! server.isTerminating()) {
            try (var socket = context.socket(type)) {
                if (socket.bind(getAddress())) {
                    log.info("Bound {} {}", type, address);
                } else {
                    log.warn("Could not bind to {}", address);
                }

                switch (type) {
                case REP:
                case ROUTER:
                    try (var poller = context.poller(1)) {
                        poller.register(socket, ZMQ.Poller.POLLIN);

                        while (! server.isTerminating()) {
                            int events = poller.poll(100);

                            if (events > 0 && poller.pollin(0)) {
                                var message = socket.recv();

                                if (message != null) {
                                    dispatch(socket, message);
                                }
                            }
                        }
                    }
                    break;

                case PUB:
                    while (! server.isTerminating()) {
                        var message = outgoing.poll(100, MILLISECONDS);

                        if (message != null) {
                            dispatch(socket, message);
                        }
                    }
                    break;

                default:
                    throw new IllegalStateException("Unsupported SocketType: " + type);
                }
            } catch (Exception exception) {
                log.warn("{}", exception);
            }
        }
    }
}
