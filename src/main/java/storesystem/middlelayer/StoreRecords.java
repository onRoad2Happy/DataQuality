package storesystem.middlelayer;

import Utils.Constants;
import Utils.DataFormatException;
import Utils.RecordsUtils;
import Utils.SLSystem;
import storesystem.underlying.LoadDataHDFS;
import storesystem.underlying.StoreDataHDFS;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StoreRecords implements StoreRecordsInterface {

    @Override
    public boolean storeRecords(String src, String database, String table) throws IOException, DataFormatException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(src));

        String str;

        int attrNum = 0;
        int recordNum = 0;
        byte[] DBName;
        byte[] TblName;
        if ((str = bufferedReader.readLine()) != null){
            DBName = str.getBytes();
        }
        else{
            return false;
        }
        if ((str = bufferedReader.readLine()) != null){
            TblName = str.getBytes();
        }
        else {
            return false;
        }

        ArrayList<byte[]> HeadInfo = new ArrayList<>();
        ArrayList<byte[]> AttrInfo = new ArrayList<>();

        // 获取属性信息 Attributes
        while((str = bufferedReader.readLine()) != null){
            String[] strs = str.split(RecordsUtils.headSplitLabel);
            if (strs[0].equals("@data")){
                break ;
            }
            else if (strs[0].equals("@attributes")){
                if (strs.length != 3){
//                    System.out.println(strs.length);
                    throw new DataFormatException("DataFormat Error! Attributes num is " + attrNum + 1);
                }
                if (strs[2].equals("int")){
                    AttrInfo.add(new byte[]{0});
                }
                else if (strs[2].equals("double")){
                    AttrInfo.add(new byte[]{1});
                }
                else if (strs[2].equals("string")){
                    AttrInfo.add(new byte[]{2});
                }
                else{
//                    System.out.println(strs[2]);
                    throw new DataFormatException("DataFormat Error! Attributes num is " + attrNum + 1);
                }
                AttrInfo.add(strs[1].getBytes());
//                System.out.println(strs[1]);
                attrNum ++;
            }
            else{
//                System.out.println(strs[0]);
                throw new DataFormatException("DataFormat Error!");
            }
        }

        int Offset = (6 + AttrInfo.size() / 2) * 4;
        int recordStart = 0;
        HeadInfo.add(getBytes(Offset));
        Offset += DBName.length;
        HeadInfo.add(getBytes(Offset));
        Offset += TblName.length;
        HeadInfo.add(getBytes(attrNum));
        HeadInfo.add(getBytes(recordStart));
        HeadInfo.add(getBytes(recordNum)); // 只是占据一个位置

        HeadInfo.add(getBytes(Offset));
        for (int i = 0;i < AttrInfo.size();i += 2){
            Offset += AttrInfo.get(i + 1).length + 1;
            HeadInfo.add(getBytes(Offset));
        }
        HeadInfo.add(DBName);
        HeadInfo.add(TblName);
        HeadInfo.addAll(AttrInfo);
        recordStart = Offset;
        HeadInfo.set(3, getBytes(recordStart)); // 真正记录recordStart

        ArrayList<byte[]> personHeadInfo = new ArrayList<>();
        Offset = 0;
        String dst = SLSystem.getURI(database,table);
        StoreDataHDFS storeDataHDFS = new StoreDataHDFS(dst);
        String dstHead = SLSystem.getURIHead(database, table);
        StoreDataHDFS storeDataHDFSHead = new StoreDataHDFS(dstHead + 0);
        personHeadInfo.add(getBytes(Offset));
        while ((str = bufferedReader.readLine()) != null){
            /// 暂时不划分文件存储
//            if (recordNum % Constants.SUBFILESIZE == 0){
//                storeDataHDFS.close();
//                storeDataHDFS = new StoreDataHDFS(dst + recordNum / Constants.SUBFILESIZE);
//                Offset = 0;
//            }
            byte[] tmp = getDataBytes(AttrInfo, str);
            if (tmp == null) continue;

            if (recordNum % Constants.SUBHEADSIZE == 0 && recordNum != 0){
                HeadInfo.set(4, getBytes(recordNum)); // 真正记录recordNum
                storeDataHDFSHead.storeData(HeadInfo);
                storeDataHDFSHead.storeData(personHeadInfo);
                storeDataHDFSHead.close();

                personHeadInfo = new ArrayList<>();
                storeDataHDFSHead = new StoreDataHDFS(dstHead + recordNum / Constants.SUBHEADSIZE);
                personHeadInfo.add(getBytes(Offset));
            }
            int len = tmp.length;
            recordNum ++;
            storeDataHDFS.storeData(tmp);
            Offset += len;
            personHeadInfo.add(getBytes(Offset));
        }
        HeadInfo.set(4, getBytes(recordNum)); // 真正记录recordNum

        storeDataHDFSHead.storeData(HeadInfo);
        storeDataHDFSHead.storeData(personHeadInfo);
        storeDataHDFSHead.close();

        String totalRecordsuri = SLSystem.getURITotalRecords(database, table);
        StoreDataHDFS storeDataHDFS1 = new StoreDataHDFS(totalRecordsuri);
        storeDataHDFS1.storeData(getBytes(recordNum));
        storeDataHDFS1.close();

        storeDataHDFS.close();
        return true;
    }

    private void storeData(StoreDataHDFS storeDataHDFS, ArrayList<String> datas) throws DataFormatException, IOException {
//        int attrNum = 0;
//        int recordNum = 0;
//        byte[] DBName = datas.get(0).getBytes();
//        byte[] TblName = datas.get(1).getBytes();
//        ArrayList<byte[]> HeadInfo = new ArrayList<>();
//        ArrayList<byte[]> AttrInfo = new ArrayList<>();
//        ArrayList<byte[]> BodyInfo = new ArrayList<>();
//        for (int i = 2;;i ++){
//            String[] strs = datas.get(i).split(RecordsUtils.headSplitLabel);
//            if (strs[0].equals("@data")){
//                break ;
//            }
//            else if (strs[0].equals("@attributes")){
//                if (strs.length != 3){
//                    System.out.println(strs.length);
//                    throw new DataFormatException("DataFormat Error! Attributes num is " + attrNum + 1);
//                }
//                if (strs[2].equals("int")){
//                    AttrInfo.add(new byte[]{0});
//                }
//                else if (strs[2].equals("double")){
//                    AttrInfo.add(new byte[]{1});
//                }
//                else if (strs[2].equals("string")){
//                    AttrInfo.add(new byte[]{2});
//                }
//                else{
//                    System.out.println(strs[2]);
//                    throw new DataFormatException("DataFormat Error! Attributes num is " + attrNum + 1);
//                }
//                AttrInfo.add(strs[1].getBytes());
//                attrNum ++;
//            }
//            else{
//                throw new DataFormatException("DataFormat Error!");
//            }
//        }
//
//        recordNum = datas.size() - attrNum - 3;
//
//        int BasicLocation = 4 * (5 + recordNum + attrNum);
//        int Offset = BasicLocation;
//        HeadInfo.add(getBytes(Offset));
//        Offset += DBName.length;
//        HeadInfo.add(getBytes(Offset));
//        Offset += TblName.length;
//        HeadInfo.add(getBytes(attrNum));
//        HeadInfo.add(getBytes(recordNum));
//
//        HeadInfo.add(getBytes(Offset));
//        for (int i = 0;i < AttrInfo.size();i += 2){
//            Offset += AttrInfo.get(i + 1).length + 1;
//            HeadInfo.add(getBytes(Offset));
//        }
//
//        for (int i = attrNum + 3;i < datas.size();i ++){
////            System.out.println(datas.get(i));
//            byte[] tmp = getDataBytes(AttrInfo,datas.get(i));
//            int len = tmp.length;
//            BodyInfo.add(tmp);
//            Offset += len;
//            HeadInfo.add(getBytes(Offset));
//        }
//        HeadInfo.add(DBName);
//        HeadInfo.add(TblName);
//        HeadInfo.addAll(AttrInfo);
//        HeadInfo.addAll(BodyInfo);
//
//        storeDataHDFS.storeData(HeadInfo);
    }
//
    private byte[] getDataBytes(List<byte[]> attrInfo, String s) throws DataFormatException {
        String[] strs = s.split(RecordsUtils.recordSplitLabel);
        ArrayList<Byte> arrayList = new ArrayList<>();
        if (strs.length > attrInfo.size() / 2) return null;
        for (int i = 0;i < strs.length;i ++){
            switch (attrInfo.get(i * 2)[0]){
                case 0:
                    try {
                        arrayList.addAll(arrToCol(getBytes(Integer.valueOf(strs[i]))));
                    } catch (NumberFormatException e){
                        continue;
                    }
                    break;
                case 1:
                    arrayList.addAll(arrToCol(getBytes(Double.valueOf(strs[i]))));
                    break;
                case 2:
                    List<Byte> list = arrToCol(strs[i].getBytes());
                    int len = list.size();
                    arrayList.addAll(arrToCol(getBytes(len)));
                    arrayList.addAll(list);
                    break;
                default:
                    throw new DataFormatException("Data Type Error! line : " + (i + 2));
            }
        }
        return colToArr(arrayList);
    }

    private byte[] colToArr(List<Byte> list){
        return SLSystem.byteCollectionToArray(list);
    }

    private List<Byte> arrToCol(byte[] bytes){
        return SLSystem.byteArrayToCollection(bytes);
    }

    private byte[] getBytes(int num){
        return SLSystem.intToByteArray(num);
    }

    private byte[] getBytes(double num){
        return SLSystem.doubleToByte(num);
    }
    @Override
    public int appendRecords(String record, String database, String table) throws IOException, DataFormatException {
        LoadRecords loadRecords = new LoadRecords(database, table);
        byte[] oldHead = loadRecords.getHeadAll();
        byte[] headInfo = loadRecords.getHeadInfo();
        List<byte[]> attrInfo = loadRecords.getAttrsAndType();
        int basicRecords = loadRecords.getBasicRecords();

        String totalRecords = SLSystem.getURITotalRecords(database, table);
        LoadDataHDFS loadDataHDFS = new LoadDataHDFS(totalRecords);
        byte[] totalRecordsNum = new byte[4];
        loadDataHDFS.read(0, totalRecordsNum, 0, 4);
        int totalNum = SLSystem.byteArrayToInt(totalRecordsNum, 0) + 1;
        totalRecordsNum = SLSystem.intToByteArray(totalNum);
        loadDataHDFS.destroy();
        StoreDataHDFS storeDataHDFS = new StoreDataHDFS(totalRecords);
        storeDataHDFS.storeData(totalRecordsNum);
        storeDataHDFS.close(); // 修改总记录数17-12-12
        System.arraycopy(totalRecordsNum ,0, oldHead, 16, 4); // 这四条语句用来修改总记录数

        List<byte[]> tmp = new ArrayList<>();
        for (int i = 0;i < attrInfo.size();i ++){
            byte[] tmp1 = new byte[1];
            byte[] tmp2 = new byte[attrInfo.get(i).length - 1];
            System.arraycopy(attrInfo.get(i), 0, tmp1, 0, 1);
            System.arraycopy(attrInfo.get(i), 1, tmp2, 0, tmp2.length);
            tmp.add(tmp1);
            tmp.add(tmp2);
        }

        int tmpindex = (totalNum - 1) / Constants.SUBHEADSIZE; // 存储头文件下标

        int lasInd = SLSystem.byteArrayToInt(headInfo,headInfo.length - 4);
        if (totalNum == basicRecords + 1) lasInd = 0;

        byte[] newrecord = getDataBytes(tmp, record); // 获取新纪录的byte数组
        byte[] newLen = SLSystem.intToByteArray(newrecord.length + lasInd);

        byte[] newHeadInfo = null;
        if (tmpindex != (int)((totalNum - 2) / Constants.SUBHEADSIZE)){
            newHeadInfo = new byte[Constants.INDEXLENGTH * 2];
            System.arraycopy(headInfo, headInfo.length - Constants.INDEXLENGTH, newHeadInfo, 0, Constants.INDEXLENGTH);
            System.arraycopy(newLen, 0, newHeadInfo, Constants.INDEXLENGTH, newLen.length);
        }
        else {
            newHeadInfo = new byte[headInfo.length + Constants.INDEXLENGTH];
            System.arraycopy(headInfo, 0, newHeadInfo, 0, headInfo.length);
            System.arraycopy(newLen, 0, newHeadInfo, headInfo.length, newLen.length);
        }

        ArrayList<byte[]> arrayList = new ArrayList<>();
        arrayList.add(oldHead);
        arrayList.add(newHeadInfo);
        // 存储headinfo
        String dst = SLSystem.getURIHead(database,table);

        storeDataHDFS = new StoreDataHDFS(dst + tmpindex);
        storeDataHDFS.storeData(arrayList);
        storeDataHDFS.close();
        // 存储append information

        dst = SLSystem.getURIAppend(database, table);
        StoreDataHDFS storeDataHDFS1 = new StoreDataHDFS(dst);
        storeDataHDFS1.addData(newrecord);
        storeDataHDFS1.close();

        return SLSystem.byteArrayToInt(oldHead,16);
    }

    private byte[] addN(byte[] oldHead, int num) {
        int[] tmp = SLSystem.byteArrayToIntArray(oldHead);
        for (int i = 0;i < tmp.length;i ++){
            if (tmp[i] != 0)
                tmp[i] += num;
        }
        return SLSystem.intArrayToByteArray(tmp);
    }

    private byte[] getDataBytes(byte[] oldHead, byte[] attrAll, String record) throws DataFormatException {
        ArrayList<byte[]> arrayList = new ArrayList<>();

        int attrNum = SLSystem.byteArrayToInt(oldHead,8);
        int base = SLSystem.byteArrayToInt(oldHead,16);
//        System.out.println(attrNum + " " + base);
        for (int i = 0;i < attrNum;i ++){
            int Slocation = SLSystem.byteArrayToInt(oldHead,16 + i * 4);
            int Elocation = SLSystem.byteArrayToInt(oldHead,20 + i * 4);
            byte[] tmp1 = Arrays.copyOfRange(attrAll,Slocation - base,Slocation - base + 1);
            byte[] tmp2 = Arrays.copyOfRange(attrAll,Slocation - base + 1,Elocation - base);
            arrayList.add(tmp1);
//            System.out.println(tmp1[0]);
            arrayList.add(tmp2);
        }
        return getDataBytes(arrayList,record);
    }
}