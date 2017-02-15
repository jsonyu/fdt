




package lia.util.net.common;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class NetMatcher {

    private ArrayList<InetNetwork> networks;

    public void initInetNetworks(final Collection<String> nets) {
        networks = new ArrayList<InetNetwork>();
        for (final String netName : nets)
            try {
                InetNetwork net = InetNetwork.getFromString(netName);
                if (!networks.contains(net)) networks.add(net);
            } catch (java.net.UnknownHostException uhe) {
                log("Cannot resolve address: " + uhe.getMessage());
            }
        networks.trimToSize();
    }

    public void initInetNetworks(final String[] nets) {
        networks = new ArrayList<InetNetwork>();
        for (int i = 0; i < nets.length; i++)
            try {
                InetNetwork net = InetNetwork.getFromString(nets[i]);
                if (!networks.contains(net)) networks.add(net);
            } catch (java.net.UnknownHostException uhe) {
                log("Cannot resolve address: " + uhe.getMessage());
            }
        networks.trimToSize();
    }

    public boolean matchInetNetwork(final String hostIP) {
        InetAddress ip = null;

        try {
            ip = InetAddress.getByName(hostIP);
        } catch (java.net.UnknownHostException uhe) {
            log("Cannot resolve address for " + hostIP + ": " + uhe.getMessage());
        }

        boolean sameNet = false;

        if (ip != null) for (Iterator iter = networks.iterator(); (!sameNet) && iter.hasNext();) {
            InetNetwork network = (InetNetwork) iter.next();
            sameNet = network.contains(ip);
        }
        return sameNet;
    }

    public boolean matchInetNetwork(final InetAddress ip) {
        boolean sameNet = false;

        for (Iterator iter = networks.iterator(); (!sameNet) && iter.hasNext();) {
            InetNetwork network = (InetNetwork) iter.next();
            sameNet = network.contains(ip);
        }
        return sameNet;
    }

    public NetMatcher() {
    }

    public NetMatcher(final String[] nets) {
        initInetNetworks(nets);
    }

    public NetMatcher(final Collection nets) {
        initInetNetworks(nets);
    }

    public String toString() {
        return networks.toString();
    }

    protected void log(String s) {
    }
}

class InetNetwork {

    

    private InetAddress network;

    private InetAddress netmask;

    public InetNetwork(InetAddress ip, InetAddress netmask) {
        network = maskIP(ip, netmask);
        this.netmask = netmask;
    }

    public boolean contains(final String name) throws java.net.UnknownHostException {
        return network.equals(maskIP(InetAddress.getByName(name), netmask));
    }

    public boolean contains(final InetAddress ip) {
        return network.equals(maskIP(ip, netmask));
    }

    public String toString() {
        return network.getHostAddress() + "/" + netmask.getHostAddress();
    }

    public int hashCode() {
        return maskIP(network, netmask).hashCode();
    }

    public boolean equals(Object obj) {
        return (obj != null) && (obj instanceof InetNetwork) && ((((InetNetwork) obj).network.equals(network)) && (((InetNetwork) obj).netmask.equals(netmask)));
    }

    public static InetNetwork getFromString(String netspec) throws java.net.UnknownHostException {
        if (netspec.endsWith("*"))
            netspec = normalizeFromAsterisk(netspec);
        else {
            int iSlash = netspec.indexOf('/');
            if (iSlash == -1)
                netspec += "/255.255.255.255";
            else if (netspec.indexOf('.', iSlash) == -1) netspec = normalizeFromCIDR(netspec);
        }

        return new InetNetwork(InetAddress.getByName(netspec.substring(0, netspec.indexOf('/'))), InetAddress.getByName(netspec.substring(netspec.indexOf('/') + 1)));
    }

    public static final InetAddress maskIP(final byte[] ip, final byte[] mask) {
        try {
            return getByAddress(new byte[] { (byte) (mask[0] & ip[0]), (byte) (mask[1] & ip[1]), (byte) (mask[2] & ip[2]), (byte) (mask[3] & ip[3])});
        } catch (final Exception _) {
        }
        return null;
    }

    public static InetAddress maskIP(final InetAddress ip, final InetAddress mask) {
        return maskIP(ip.getAddress(), mask.getAddress());
    }

    
    static private String normalizeFromAsterisk(final String netspec) {
        String[] masks = { "0.0.0.0/0.0.0.0", "0.0.0/255.0.0.0", "0.0/255.255.0.0", "0/255.255.255.0"};
        char[] srcb = netspec.toCharArray();
        int octets = 0;
        for (int i = 1; i < netspec.length(); i++) {
            if (srcb[i] == '.') octets++;
        }
        return (octets == 0) ? masks[0] : netspec.substring(0, netspec.length() - 1).concat(masks[octets]);
    }

    
    static private String normalizeFromCIDR(final String netspec) {
        final int bits = 32 - Integer.parseInt(netspec.substring(netspec.indexOf('/') + 1));
        final int mask = (bits == 32) ? 0 : 0xFFFFFFFF - ((1 << bits) - 1);

        return netspec.substring(0, netspec.indexOf('/') + 1) + Integer.toString(mask >> 24 & 0xFF, 10) + "." + Integer.toString(mask >> 16 & 0xFF, 10) + "." + Integer.toString(mask >> 8 & 0xFF, 10) + "." + Integer.toString(mask >> 0 & 0xFF, 10);
    }

    private static java.lang.reflect.Method getByAddress = null;

    static {
        try {
            Class inetAddressClass = Class.forName("java.net.InetAddress");
            Class[] parameterTypes = { byte[].class};
            getByAddress = inetAddressClass.getMethod("getByAddress", parameterTypes);
        } catch (Exception e) {
            getByAddress = null;
        }
    }

    private static InetAddress getByAddress(byte[] ip) throws java.net.UnknownHostException {
        InetAddress addr = null;
        if (getByAddress != null) try {
            addr = (InetAddress) getByAddress.invoke(null, new Object[] { ip});
        } catch (IllegalAccessException e) {
        } catch (java.lang.reflect.InvocationTargetException e) {
        }

        if (addr == null) {
            addr = InetAddress.getByName(Integer.toString(ip[0] & 0xFF, 10) + "." + Integer.toString(ip[1] & 0xFF, 10) + "." + Integer.toString(ip[2] & 0xFF, 10) + "." + Integer.toString(ip[3] & 0xFF, 10));
        }
        return addr;
    }

    
    public static void main(String[] args) {
        NetMatcher nm = new NetMatcher();
        nm.initInetNetworks(new String[] { "192.168.0.0/24"});
        System.out.println(nm.matchInetNetwork("192.168.0.2"));

    }
}
