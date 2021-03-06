package fi.kroon.vadret.data.aggregatedfeed

import dagger.Lazy
import fi.kroon.vadret.data.aggregatedfeed.model.AggregatedFeed
import fi.kroon.vadret.data.aggregatedfeed.net.AggregatedFeedNetDataSource
import fi.kroon.vadret.data.exception.ErrorHandler
import fi.kroon.vadret.data.exception.ExceptionHandler
import fi.kroon.vadret.data.exception.IErrorHandler
import fi.kroon.vadret.data.exception.IExceptionHandler
import fi.kroon.vadret.data.failure.Failure
import fi.kroon.vadret.di.scope.CoreApplicationScope
import fi.kroon.vadret.util.NetworkHandler
import fi.kroon.vadret.util.extension.asLeft
import fi.kroon.vadret.util.extension.asRight
import io.github.sphrak.either.Either
import io.reactivex.Single
import javax.inject.Inject
import retrofit2.Response

@CoreApplicationScope
class AggregatedFeedRepository @Inject constructor(
    private val networkHandler: NetworkHandler,
    private val netDataSource: Lazy<AggregatedFeedNetDataSource>,
    private val errorHandler: ErrorHandler,
    private val exceptionHandler: ExceptionHandler
) : IErrorHandler by errorHandler, IExceptionHandler<Failure> by exceptionHandler { //#super_delegation,super_delegation

    /**
     *  When [response.body] is null [NetworkResponseEmpty] is
     *  returned.
     */
    operator fun invoke(feeds: String, counties: String): Single<Either<Failure, List<AggregatedFeed>>> = //#overloaded_op
        when (networkHandler.isConnected) {//#when_expr
            true -> {
                netDataSource
                    .get()
                    .getAggregatedFeed(//#func_call_with_named_arg
                        feeds = feeds,
                        counties = counties
                    ).map { response: Response<List<AggregatedFeed>> -> //#lambda
                        response
                            .body()
                            ?.asRight() //#safe_call
                            ?: Failure.NetworkResponseEmpty
                                .asLeft()
                    }
            }
            false -> getNetworkOfflineError()
        }.onErrorReturn { //#lambda
            exceptionHandler(it)
                .asLeft()
        }
}
