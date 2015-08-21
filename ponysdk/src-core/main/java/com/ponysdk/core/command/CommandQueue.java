
package com.ponysdk.core.command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandQueue<T> {

    private static final Logger log = LoggerFactory.getLogger(CommandQueue.class);

    private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final List<Callable<CommandResult<T>>> queue = new ArrayList<>();

    public CommandQueue() {}

    public void addCommand(final Command<T> command) {
        queue.add(new CallableCommand<>(command));
    }

    public List<CommandResult<T>> execute() {

        final List<CommandResult<T>> results = new ArrayList<>();
        List<Future<CommandResult<T>>> all;
        try {
            all = executorService.invokeAll(queue);
            for (final Future<CommandResult<T>> future : all) {
                results.add(future.get());
            }
        } catch (final InterruptedException e1) {
            log.error("", e1);
        } catch (final ExecutionException e1) {
            log.error("", e1);
        }

        return results;
    }

    private class CallableCommand<R> implements Callable<CommandResult<R>> {

        private final Command<R> command;

        public CallableCommand(final Command<R> command) {
            this.command = command;
        }

        @Override
        public CommandResult<R> call() throws Exception {
            CommandResult<R> asyncResult = null;
            try {
                final R result = command.execute();
                asyncResult = new CommandResult<>(result);
            } catch (final Throwable e) {
                asyncResult = new CommandResult<>(e);
            }
            return asyncResult;
        }

    }
}
