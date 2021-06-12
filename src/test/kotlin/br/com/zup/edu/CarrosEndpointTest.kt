package br.com.zup.edu

import io.grpc.Channel
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Singleton

@MicronautTest(transactional = false) // O gRPC por padrao faz suas chamadas remotas em uma thread separada, false desabilita isso
internal class CarrosEndpointTest(
    val repository: CarroRepository,
    val grpcClient: CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub){

    @BeforeEach
    fun antesDeCadaTeste(){
        repository.deleteAll()
    }

    @Test
    fun `deve adicionar um novo carro`(){

        val response = grpcClient.adcionar(CarrosRequest.newBuilder().setModelo("Gol").setPlaca("HPX-1234").build())

        //assertNotNull(response.id)
        //assertTrue(repository.existsById(response.id))
        with(response){
            assertNotNull(id)
            assertTrue(repository.existsById(id))
        }
    }

    @Test
    fun `nao deve adicionar um novo carro quando placa ja existir`(){

        val carroExistente = repository.save(Carro("Palio", "OPI-9886"))

        val error = assertThrows<StatusRuntimeException>{
            // Vai acontecer na mesma thread da transacao, devido false lá em cima na classe, MicronautTest(transactional = false)
            grpcClient.adcionar(CarrosRequest.newBuilder()
                .setModelo("Ferrari")
                .setPlaca(carroExistente.placa).build())
        }

        with(error){
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Placa existente", status.description)
        }
    }

    @Test
    fun `nao deve adicionar um novo carro quando dados de entrada invalidos`(){

        val error = assertThrows<StatusRuntimeException>{
            // Vai acontecer na mesma thread da transacao, devido false lá em cima na classe, MicronautTest(transactional = false)
            grpcClient.adcionar(CarrosRequest.newBuilder()
                .setModelo("")
                .setPlaca("")
                .build())
        }

        with(error){
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados de entrada inválidos", status.description)
            // TODO: Verificar as violações da bean validations
        }
    }

    @Factory
    class Clients{
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub?{
            return CarrosGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}