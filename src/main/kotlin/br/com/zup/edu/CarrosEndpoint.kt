package br.com.zup.edu

import io.grpc.Status
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
class CarrosEndpoint(@Inject val repository: CarroRepository): CarrosGrpcServiceGrpc.CarrosGrpcServiceImplBase() {
    override fun adcionar(request: CarrosRequest?,
                          responseObserver: StreamObserver<CarrosResponse>?) {

        if(repository.existsByPlaca(request!!.placa)){
            responseObserver!!.onError(Status.ALREADY_EXISTS
                .withDescription("Placa existente")
                .asRuntimeException())
            return
        }

        val carro = Carro(request.modelo, request.placa)

        try {
            repository.save(carro)
        }catch (e: ConstraintViolationException){
            responseObserver!!.onError(Status.INVALID_ARGUMENT
                .withDescription("Dados de entrada inválidos")
                .asRuntimeException())
            return
        }

        responseObserver!!.onNext(CarrosResponse.newBuilder().setId(carro.id!!).build())
        responseObserver.onCompleted()
    }
}