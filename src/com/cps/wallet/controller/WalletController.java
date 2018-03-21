package com.cps.wallet.controller;

import com.cps.wallet.dao.UserDao;
import com.cps.wallet.entity.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDUtils;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.web3j.abi.ERC20Token;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.Key;
import java.util.*;

import static org.web3j.tx.Contract.GAS_LIMIT;
import static org.web3j.tx.ManagedTransaction.GAS_PRICE;

/**
 * Created by fengss on 2018/3/14.
 */
@Api
@Controller
@RequestMapping("/wallet")
public class WalletController {

    private static final String ivParameter = "5!@76#%^10odkABG";

    private static final String encryptAlgorithm = "AES";

    private static final String encryptType = "AES/CBC/ISO10126Padding";

    Credentials credentials;
    ERC20Token cps;
    String contractAddress = "0x0E3E4BfD5a2572c1E0475029D43Ac0D274466017";
    String transferFuncHash = "";
    Function transferFunc = null;
    Web3j web3j;

    private String web3Url;

    @Value("${contractAddress}")
    public void setContractAddress(String address){
        this.contractAddress = address;
    }

    @Value("${web3_url}")
    public void setWeb3Url(String url){
        web3Url = url;

        web3j = Web3j.build(new HttpService(web3Url));

        credentials = Credentials.create("3a1076bf45ab87712ad64ccb3b10217737f7faacbf2872e88fdd9a537d8fe266");
        cps = ERC20Token.load(contractAddress, web3j, credentials, GAS_PRICE, GAS_LIMIT);
    }

    @Autowired
    private UserDao userDao;

    private static Map<Integer,String> map=new HashMap<Integer,String>();

    @ApiOperation("logout")
    @RequestMapping(value = "logout", method = RequestMethod.POST)
    public @ResponseBody Response<Integer> loginout(@ApiParam @RequestBody Logout logout){
        Response<Integer> response = new Response<>();
        try {
            map.remove(logout.getUserId());
            response.setErrorCode(0);
            response.setErrorMsg("logout successed");
        }catch(Exception e){
            e.printStackTrace();
            response.setErrorCode(-1);
            response.setErrorMsg("logout error");
        }
        return response;
    }


    @ApiOperation("login")
    @RequestMapping(value = "login", method = RequestMethod.POST)
    public @ResponseBody Response<LoginResponse> login(@ApiParam @RequestBody LoginRequest loginRequest){
        Response<LoginResponse> response = new Response<>();
        Integer userId = checkPassWord(loginRequest.getUsername(),loginRequest.getPassword());
        if(userId > 0){
            String sessionId = userId.toString()+String.valueOf(new Date().getTime());
            map.put(userId,sessionId);
            LoginResponse loginResponse = new LoginResponse();
            List<String> list =userDao.getUserAdderss(userId);
            Map<String,BigInteger> map = new HashMap<String,BigInteger>();
            for(String address : list){
                Web3j web3j = Web3j.build(new HttpService(web3Url));
                try {
                    EthGetBalance ethGetBalance =  web3j.ethGetBalance(address,DefaultBlockParameterName.LATEST).send();
                    map.put(address,ethGetBalance.getBalance());
                } catch (IOException e) {
                    e.printStackTrace();
                    map.put(address, BigInteger.valueOf(-1));
                }
            }
            loginResponse.setUserId(userId);
            loginResponse.setSessionId(sessionId);
            loginResponse.setMap(map);
            response.setModel(loginResponse);
            response.setErrorCode(0);
        }else{
            response.setErrorCode(-1);
            response.setErrorMsg("login error");
        }
        return response;
    }

    @ApiOperation("createUser")
    @RequestMapping(value = "create/user", method = RequestMethod.POST)
    @ResponseBody
    public Response<Integer> createUser(@ApiParam @RequestBody UserInfo userInfo){
        Response<Integer> response = new Response<>();
        Integer i = userDao.createUser(userInfo.getUsername(),userInfo.getPassword(),userInfo.getMobile());
        if(i>0){
            response.setModel(i);
            response.setErrorCode(0);
        }else{
            response.setErrorCode(-1);
            response.setErrorMsg("create user error");
        }
        return response;
    }

    @ApiOperation("view balance")
    @RequestMapping(value = "view/balance", method = RequestMethod.GET)
    @ResponseBody
    public Response<BigInteger> viewBalance(@ApiParam @RequestParam(name="address")String address){
        Response<BigInteger> response = new Response<>();
        Web3j web3j = Web3j.build(new HttpService(web3Url));
        try {
            EthGetBalance ethGetBalance =  web3j.ethGetBalance(address,DefaultBlockParameterName.LATEST).send();
            response.setModel(ethGetBalance.getBalance());
            response.setErrorCode(0);
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            response.setErrorCode(0);
            response.setErrorMsg("view balance error");
            return response;
        }
    }

    @ApiOperation("view cps balance")
    @RequestMapping(value = "view/cpsbalance", method = RequestMethod.GET)
    @ResponseBody
    public Response<BigInteger> viewCPSBalance(@ApiParam @RequestParam(name="address")String address){
        Response<BigInteger> response = new Response<>();
        try {
            BigInteger balance = cps.balanceOf(address).send();
            response.setModel(balance);
            response.setErrorCode(0);
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            response.setErrorCode(-1);
            response.setErrorMsg("view balance error");
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.setErrorCode(-1);
            response.setErrorMsg(e.getLocalizedMessage());
            return response;
        }
    }

    @ApiOperation("transfer")
    @RequestMapping(value = "transfer", method = RequestMethod.POST)
    @ResponseBody
    public Response<String> transfer(@ApiParam @RequestBody TransferRequest transferRequest){
        Response<String> response = new Response<>();
        String privKey = getPrivKey(transferRequest.getFrom(),transferRequest.getPassword());
        Credentials credentialsM = Credentials.create(privKey);
        Web3j web3j = Web3j.build(new HttpService(web3Url));
        Convert.Unit unit = Convert.Unit.WEI;
        if(transferRequest.getUnit() != null){
            if("kwei".equals(transferRequest.getUnit())){
                unit = Convert.Unit.KWEI;
            }else if("mwei".equals(transferRequest.getUnit())){
                unit = Convert.Unit.MWEI;
            }else if("gwei".equals(transferRequest.getUnit())){
                unit = Convert.Unit.GWEI;
            }else if("szabo".equals(transferRequest.getUnit())){
                unit = Convert.Unit.SZABO;
            }else if("finney".equals(transferRequest.getUnit())){
                unit = Convert.Unit.FINNEY;
            }else if("ether".equals(transferRequest.getUnit())){
                unit = Convert.Unit.ETHER;
            }else if("kether".equals(transferRequest.getUnit())){
                unit = Convert.Unit.KETHER;
            }else if("mether".equals(transferRequest.getUnit())){
                unit = Convert.Unit.METHER;
            }else if("gether".equals(transferRequest.getUnit())){
                unit = Convert.Unit.GETHER;
            }
        }else{
            unit = Convert.Unit.WEI;
        }
        try {
            boolean c = checkSession(Integer.parseInt(transferRequest.getUserId()),transferRequest.getSessionId());
            boolean b = checkPassWord(Integer.valueOf(transferRequest.getUserId()),transferRequest.getPassword());
            if( !b || !c){
                response.setErrorCode(-1);
                response.setErrorMsg("session or password error");
                response.setModel("密码错误或者sessionid错误");
                return response;
            }
            TransactionReceipt transactionReceipt = Transfer.sendFunds(web3j,credentialsM,transferRequest.getTo(),BigDecimal.valueOf(Long.valueOf(transferRequest.getAmount())),unit).send();
            response.setErrorCode(0);
            response.setModel(transactionReceipt.getTransactionHash());
            return response;
            //System.out.println("Transaction complete, view it at https://rinkeby.etherscan.io/tx/" + transactionReceipt.getTransactionHash());
        } catch (Exception e) {
            e.printStackTrace();
            response.setErrorCode(-1);
            response.setErrorMsg(e.getLocalizedMessage());
            return response;
        }
    }

    @ApiOperation("transfer cps")
    @RequestMapping(value = "transfercps", method = RequestMethod.POST)
    @ResponseBody
    public Response<String> transferCps(@ApiParam @RequestBody TransferRequest transferRequest){
        Response<String> response = new Response<>();
        String privKey = getPrivKey(transferRequest.getFrom(),transferRequest.getPassword());
        Credentials credentials = Credentials.create(privKey);
        Web3j web3j = Web3j.build(new HttpService(web3Url));

        try {
            boolean c = checkSession(Integer.parseInt(transferRequest.getUserId()),transferRequest.getSessionId());
            boolean b = checkPassWord(Integer.valueOf(transferRequest.getUserId()),transferRequest.getPassword());
            if( !b || !c){
                response.setErrorCode(-1);
                response.setErrorMsg("session or password error");
                response.setModel("密码错误或者sessionid错误");
                return response;
            }

            ERC20Token cps = ERC20Token.load(contractAddress, web3j, credentials, GAS_PRICE, GAS_LIMIT);
            TransactionReceipt transactionReceipt = cps.transfer(transferRequest.getTo(), new BigInteger(transferRequest.getAmount())).send();

            response.setErrorCode(0);
            response.setModel(transactionReceipt.getTransactionHash());
            return response;
            //System.out.println("Transaction complete, view it at https://rinkeby.etherscan.io/tx/" + transactionReceipt.getTransactionHash());
        } catch (Exception e) {
            e.printStackTrace();
            response.setErrorCode(-1);
            response.setErrorMsg(e.getLocalizedMessage());
            return response;
        }
    }

    @ApiOperation("list all wallets")
    @RequestMapping(value = "list", method = RequestMethod.POST)
    @ResponseBody
    public Response<List<String>> listWallet(@ApiParam @RequestBody Mnemonics mnemonics) {
        Response<List<String>> response = new Response<>();
        if (!checkSession(mnemonics.getUserId(), mnemonics.getSessionId())) {
            response.setErrorCode(-1);
            response.setErrorMsg("session error");

            return response;
        }

        try {
            response.setModel(userDao.walletAddresses(mnemonics.getUserId()));
            response.setErrorCode(0);
            return response;
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setErrorCode(-1);
            response.setErrorMsg(e.getLocalizedMessage());
            return response;
        }
    }

    @ApiOperation("import mnemonics and restore wallet")
    @RequestMapping(value = "import", method = RequestMethod.POST)
    @ResponseBody
    public Response<String> importWallet(@ApiParam @RequestBody Mnemonics mnemonics){
        Response<String> response = new Response<>();
        if(!checkSession(mnemonics.getUserId(),mnemonics.getSessionId())){
            response.setErrorCode(-1);
            response.setErrorMsg("session error");
            return response;
        }

        List<String> mnemonicsList = new ArrayList<String>();
        for(String m : mnemonics.getMnemonics().split(" ")){
            if(m.length() > 0)mnemonicsList.add(m);
        }
// BitcoinJ
        DeterministicSeed seed = new DeterministicSeed(mnemonicsList, null, "", new Date().getTime());
        DeterministicKeyChain chain = DeterministicKeyChain.builder().seed(seed).build();
        List<ChildNumber> keyPath = HDUtils.parsePath("M/44H/60H/0H/0/0");
        DeterministicKey key = chain.getKeyByPath(keyPath, true);
        BigInteger privKey = key.getPrivKey();
// Web3j
        Credentials credentialsM = Credentials.create(privKey.toString(16));
        String address = credentialsM.getAddress();
        //需要将私钥进行加密
        String str = encrypt(privKey.toString(16),mnemonics.getPassword());
        Integer rowsAffected =  userDao.importWallet(mnemonics.getUserId(), address, str,encrypt(mnemonics.getMnemonics(),mnemonics.getPassword()));
        if(rowsAffected != null && rowsAffected>0){
            response.setErrorCode(0);
            response.setModel(address);
            return response;
        }else{
            response.setErrorCode(-1);
            response.setErrorMsg("导入钱包失败");
            return response;
        }
    }

    @ApiOperation("export wallet")
    @RequestMapping(value = "export", method = RequestMethod.POST)
    @ResponseBody
    public Response<String> exportWallet(@ApiParam @RequestBody MnemonicsExport mnemonicsExport){
        Response<String> response = new Response<>();
        if(!checkSession(mnemonicsExport.getUserId(),mnemonicsExport.getSessionId())){
            response.setErrorCode(-1);
            response.setErrorMsg("session错误");
            return response;
        }

        Mnemonics mnemonics =  userDao.exportWallet(mnemonicsExport.getUserId(), mnemonicsExport.getAddress());
        if(mnemonics != null && mnemonics.getMnemonics() != null){

            response.setErrorCode(0);
            String mnemonicsStr = decrypt(mnemonics.getMnemonics(), mnemonicsExport.getPassword());
            if(mnemonicsStr == null || mnemonicsStr.length() == 0){
                response.setModel(mnemonicsStr);
                response.setErrorCode(-1);
                response.setErrorMsg("密码错误");
            }
            else {
                response.setModel(mnemonicsStr);
            }


            return response;
        }else{
            response.setErrorCode(-1);
            response.setErrorMsg("出钱包失败");
            return response;
        }
    }

    public String getPrivKey(String from,String password){
        //需要解密
        return decrypt(userDao.getPrivKey(from), password);
    }

    public Integer checkPassWord(String username,String password){
        return userDao.checkPassWord(username,password);
    }

    public boolean checkPassWord(Integer userId,String password){
            return userDao.checkPassWord(userId,password);
    }

    public static String encrypt(String content, String password) {
        byte[] plain;
        String cipherText = null;
        try {
            plain = content.getBytes(Hex.DEFAULT_CHARSET_NAME);
            byte[] pwd = Hex.decodeHex(password.toCharArray());
            byte[] ivBytes = ivParameter.getBytes(Hex.DEFAULT_CHARSET_NAME);


            Key key = new SecretKeySpec((password+"0000000000").getBytes(), encryptAlgorithm);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(encryptType);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);//
            byte[] cipherBlock = cipher.doFinal(plain);

            cipherText = Base64.encodeBase64String(cipherBlock);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return cipherText;
    }

    public static String decrypt(String content, String password) {
        String cipherText = null;
        byte[] ivBytes;
        try {
            byte[] plain = Base64.decodeBase64(content);
            byte[] pwd = Hex.decodeHex(password.toCharArray());
            ivBytes = ivParameter.getBytes(Hex.DEFAULT_CHARSET_NAME);
            Key key = new SecretKeySpec((password+"0000000000").getBytes(), encryptAlgorithm);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(encryptType);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);//
            byte[] cipherBlock = cipher.doFinal(plain); //
            cipherText = new String(cipherBlock, Hex.DEFAULT_CHARSET_NAME);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return cipherText;
    }
    public boolean checkSession(Integer userId,String sessionId){
        String str = map.get(userId);
        if(sessionId.equals(str))
            return true;
        else
            return false;
    }

}
