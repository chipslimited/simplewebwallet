//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.web3j.abi;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.ManagedTransaction;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.exceptions.ContractCallException;
import org.web3j.utils.Numeric;

public abstract class Contract extends ManagedTransaction {
    public static final BigInteger GAS_LIMIT = BigInteger.valueOf(4300000L);
    protected final String contractBinary;
    protected String contractAddress;
    protected BigInteger gasPrice;
    protected BigInteger gasLimit;
    protected TransactionReceipt transactionReceipt;
    protected Map<String, String> deployedAddresses;

    protected Contract(String contractBinary, String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(web3j, transactionManager);
        this.contractAddress = contractAddress;
        this.contractBinary = contractBinary;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
    }

    protected Contract(String contractBinary, String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        this(contractBinary, contractAddress, web3j, (TransactionManager)(new RawTransactionManager(web3j, credentials)), gasPrice, gasLimit);
    }

    /** @deprecated */
    @Deprecated
    protected Contract(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        this("", contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    /** @deprecated */
    @Deprecated
    protected Contract(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        this("", contractAddress, web3j, (TransactionManager)(new RawTransactionManager(web3j, credentials)), gasPrice, gasLimit);
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getContractAddress() {
        return this.contractAddress;
    }

    public void setTransactionReceipt(TransactionReceipt transactionReceipt) {
        this.transactionReceipt = transactionReceipt;
    }

    public String getContractBinary() {
        return this.contractBinary;
    }

    public boolean isValid() throws IOException {
        if (this.contractAddress.equals("")) {
            throw new UnsupportedOperationException("Contract binary not present, you will need to regenerate your smart contract wrapper with web3j v2.2.0+");
        } else {
            EthGetCode ethGetCode = (EthGetCode)this.web3j.ethGetCode(this.contractAddress, DefaultBlockParameterName.LATEST).send();
            if (ethGetCode.hasError()) {
                return false;
            } else {
                String code = Numeric.cleanHexPrefix(ethGetCode.getCode());
                return !code.isEmpty() && this.contractBinary.contains(code);
            }
        }
    }

    public Optional<TransactionReceipt> getTransactionReceipt() {
        return Optional.ofNullable(this.transactionReceipt);
    }

    private List<Type> executeCall(Function function) throws IOException {
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall ethCall = (EthCall)this.web3j.ethCall(Transaction.createEthCallTransaction(this.transactionManager.getFromAddress(), this.contractAddress, encodedFunction), DefaultBlockParameterName.LATEST).send();
        String value = ethCall.getValue();
        return FunctionReturnDecoder.decode(value, function.getOutputParameters());
    }

    protected <T extends Type> T executeCallSingleValueReturn(Function function) throws IOException {
        List<Type> values = this.executeCall(function);
        return !values.isEmpty() ? (T) values.get(0) : null;
    }

    protected <T extends Type, R> R executeCallSingleValueReturn(Function function, Class<R> returnType) throws IOException {
        T result = this.executeCallSingleValueReturn(function);
        if (result == null) {
            throw new ContractCallException("Empty value (0x) returned from contract");
        } else {
            Object value = result.getValue();
            if (returnType.isAssignableFrom(value.getClass())) {
                return (R) value;
            } else if (result.getClass().equals(Address.class) && returnType.equals(String.class)) {
                return (R) result.toString();
            } else {
                throw new ContractCallException("Unable to convert response: " + value + " to expected type: " + returnType.getSimpleName());
            }
        }
    }

    protected List<Type> executeCallMultipleValueReturn(Function function) throws IOException {
        return this.executeCall(function);
    }

    protected TransactionReceipt executeTransaction(Function function) throws IOException, TransactionException {
        return this.executeTransaction(function, BigInteger.ZERO);
    }

    private TransactionReceipt executeTransaction(Function function, BigInteger weiValue) throws IOException, TransactionException {
        return this.executeTransaction(FunctionEncoder.encode(function), weiValue);
    }

    TransactionReceipt executeTransaction(String data, BigInteger weiValue) throws TransactionException, IOException {
        return this.send(this.contractAddress, data, weiValue, this.gasPrice, this.gasLimit);
    }

    protected <T extends Type> RemoteCall<T> executeRemoteCallSingleValueReturn(Function function) {
        return new RemoteCall(() -> {
            return this.executeCallSingleValueReturn(function);
        });
    }

    protected <T> RemoteCall<T> executeRemoteCallSingleValueReturn(Function function, Class<T> returnType) {
        return new RemoteCall(() -> {
            return this.executeCallSingleValueReturn(function, returnType);
        });
    }

    protected RemoteCall<List<Type>> executeRemoteCallMultipleValueReturn(Function function) {
        return new RemoteCall(() -> {
            return this.executeCallMultipleValueReturn(function);
        });
    }

    protected RemoteCall<TransactionReceipt> executeRemoteCallTransaction(Function function) {
        return new RemoteCall(() -> {
            return this.executeTransaction(function);
        });
    }

    protected RemoteCall<TransactionReceipt> executeRemoteCallTransaction(Function function, BigInteger weiValue) {
        return new RemoteCall(() -> {
            return this.executeTransaction(function, weiValue);
        });
    }

    private static <T extends Contract> T create(T contract, String binary, String encodedConstructor, BigInteger value) throws IOException, TransactionException {
        TransactionReceipt transactionReceipt = contract.executeTransaction(binary + encodedConstructor, value);
        String contractAddress = transactionReceipt.getContractAddress();
        if (contractAddress == null) {
            throw new RuntimeException("Empty contract address returned");
        } else {
            contract.setContractAddress(contractAddress);
            contract.setTransactionReceipt(transactionReceipt);
            return contract;
        }
    }

    protected static <T extends Contract> T deploy(Class<T> type, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String binary, String encodedConstructor, BigInteger value) throws IOException, TransactionException {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor(String.class, Web3j.class, Credentials.class, BigInteger.class, BigInteger.class);
            constructor.setAccessible(true);
            T contract = (T) constructor.newInstance(null, web3j, credentials, gasPrice, gasLimit);
            return create(contract, binary, encodedConstructor, value);
        } catch (Exception var10) {
            throw new RuntimeException(var10);
        }
    }

    protected static <T extends Contract> T deploy(Class<T> type, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String binary, String encodedConstructor, BigInteger value) throws IOException, TransactionException {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor(String.class, Web3j.class, TransactionManager.class, BigInteger.class, BigInteger.class);
            constructor.setAccessible(true);
            T contract = (T) constructor.newInstance(null, web3j, transactionManager, gasPrice, gasLimit);
            return create(contract, binary, encodedConstructor, value);
        } catch (Exception var10) {
            throw new RuntimeException(var10);
        }
    }

    protected static <T extends Contract> RemoteCall<T> deployRemoteCall(Class<T> type, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String binary, String encodedConstructor, BigInteger value) {
        return new RemoteCall(() -> {
            return deploy(type, web3j, credentials, gasPrice, gasLimit, binary, encodedConstructor, value);
        });
    }

    protected static <T extends Contract> RemoteCall<T> deployRemoteCall(Class<T> type, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String binary, String encodedConstructor) {
        return deployRemoteCall(type, web3j, credentials, gasPrice, gasLimit, binary, encodedConstructor, BigInteger.ZERO);
    }

    protected static <T extends Contract> RemoteCall<T> deployRemoteCall(Class<T> type, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String binary, String encodedConstructor, BigInteger value) {
        return new RemoteCall(() -> {
            return deploy(type, web3j, transactionManager, gasPrice, gasLimit, binary, encodedConstructor, value);
        });
    }

    protected static <T extends Contract> RemoteCall<T> deployRemoteCall(Class<T> type, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String binary, String encodedConstructor) {
        return deployRemoteCall(type, web3j, transactionManager, gasPrice, gasLimit, binary, encodedConstructor, BigInteger.ZERO);
    }

    public static EventValues staticExtractEventParameters(Event event, Log log) {
        List<String> topics = log.getTopics();
        String encodedEventSignature = EventEncoder.encode(event);
        if (!((String)topics.get(0)).equals(encodedEventSignature)) {
            return null;
        } else {
            List<Type> indexedValues = new ArrayList();
            List<Type> nonIndexedValues = FunctionReturnDecoder.decode(log.getData(), event.getNonIndexedParameters());
            List<TypeReference<Type>> indexedParameters = event.getIndexedParameters();

            for(int i = 0; i < indexedParameters.size(); ++i) {
                Type value = FunctionReturnDecoder.decodeIndexedValue((String)topics.get(i + 1), (TypeReference)indexedParameters.get(i));
                indexedValues.add(value);
            }

            return new EventValues(indexedValues, nonIndexedValues);
        }
    }

    protected EventValues extractEventParameters(Event event, Log log) {
        return staticExtractEventParameters(event, log);
    }

    protected List<EventValues> extractEventParameters(Event event, TransactionReceipt transactionReceipt) {
        List<Log> logs = transactionReceipt.getLogs();
        List<EventValues> values = new ArrayList();
        Iterator var5 = logs.iterator();

        while(var5.hasNext()) {
            Log log = (Log)var5.next();
            EventValues eventValues = this.extractEventParameters(event, log);
            if (eventValues != null) {
                values.add(eventValues);
            }
        }

        return values;
    }

    protected String getStaticDeployedAddress(String networkId) {
        return null;
    }

    public final void setDeployedAddress(String networkId, String address) {
        if (this.deployedAddresses == null) {
            this.deployedAddresses = new HashMap();
        }

        this.deployedAddresses.put(networkId, address);
    }

    public final String getDeployedAddress(String networkId) {
        String addr = null;
        if (this.deployedAddresses != null) {
            addr = (String)this.deployedAddresses.get(networkId);
        }

        return addr == null ? this.getStaticDeployedAddress(networkId) : addr;
    }
}
