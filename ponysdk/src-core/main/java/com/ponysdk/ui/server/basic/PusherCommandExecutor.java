
package com.ponysdk.ui.server.basic;


final class PusherCommandExecutor {

    static final void execute(final PPusher pusher, final PCommand command) {
        command.execute();
    }

}